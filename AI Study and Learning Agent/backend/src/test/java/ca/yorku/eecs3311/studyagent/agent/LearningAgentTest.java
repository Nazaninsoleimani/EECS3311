package ca.yorku.eecs3311.studyagent.agent;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.agent.tools.*;
import ca.yorku.eecs3311.studyagent.domain.*;
import ca.yorku.eecs3311.studyagent.integration.llm.*;
import ca.yorku.eecs3311.studyagent.integration.retrieval.*;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class LearningAgentTest {
    private final SqliteDatabase db = SqliteDatabase.inMemory();
    private final SqliteMaterialRepository materials = new SqliteMaterialRepository(db);
    private final SqliteConceptRepository concepts = new SqliteConceptRepository(db);
    private final SqliteProgressRepository progress = new SqliteProgressRepository(db);
    private final SqliteCourseRepository courses = new SqliteCourseRepository(db);
    private final RetrievalService retrieval = new RetrievalService(new LocalHashEmbeddingService(), new SqliteVectorStore(db, materials));
    private final List<LLMRequest> log = new ArrayList<>();
    private Concept inheritance;
    private Concept polymorphism;

    LearningAgentTest() {
        courses.save(new Course("c1", "s1", "EECS3311", "Software Design", null));
        CourseMaterial m = new CourseMaterial("m1", "c1", "week5.pdf", FileType.PDF, "week5.pdf");
        List<DocumentChunk> chunks = List.of(
                new DocumentChunk("ch1", "m1", "c1", "week5.pdf", 0,
                        "Inheritance lets a subclass reuse the fields and methods of its superclass.", "p.1"),
                new DocumentChunk("ch2", "m1", "c1", "week5.pdf", 1,
                        "Polymorphism means a superclass reference can call overridden methods of the subclass at runtime.", "p.2"));
        m.markIndexed(chunks.size());
        materials.save(m);
        materials.saveChunks("m1", chunks);
        retrieval.index(m, chunks);
        inheritance = new Concept("k1", "c1", "Inheritance", "is-a reuse");
        polymorphism = new Concept("k2", "c1", "Polymorphism", "many forms");
        concepts.saveAll("c1", List.of(inheritance, polymorphism));
        concepts.saveRelations("c1", List.of(new ConceptRelation("c1", "k1", "k2", RelationType.PREREQUISITE_OF)));
    }

    private LearningAgent agent(LLMClient llm) {
        ContextBuilder cb = new ContextBuilder(4000);
        ResponseValidator v = new ResponseValidator();
        ToolRegistry tools = new ToolRegistry();
        tools.register(new RetrievalTool(retrieval));
        tools.register(new ProgressAnalysisTool(concepts, progress));
        tools.register(new StudyScheduleTool());
        tools.register(new QuizGenerationTool(llm, cb, v));
        tools.register(new FlashcardGenerationTool(llm, cb, v));
        tools.register(new SummarizationTool(llm, cb, v));
        tools.register(new ConceptExtractionTool(llm, cb, v));
        MemoryManager memory = new MemoryManager(new SqliteStudentRepository(db), courses, progress,
                new SqliteLearningHistoryRepository(db), 20);
        return new LearningAgent(new Planner(llm, cb), tools, memory, cb, v, llm, materials, concepts, progress);
    }

    private LLMClient fake(Function<LLMRequest, String> structured, Function<LLMRequest, String> plain) {
        return GenerationToolsTest.fake(structured, plain, log);
    }

    @Test
    void answerIsGroundedInRetrievedMaterialAndRemembered() {
        LearningAgent a = agent(fake(r -> "{}", r -> "A subclass reuses superclass members [1]."));
        Answer ans = a.answerQuestion("s1", "sess", "c1", "What does inheritance let a subclass do?");
        assertTrue(ans.grounded);
        assertEquals(1, ans.citations.size());
        assertTrue(ans.citations.get(0).contains("week5.pdf"));
        assertTrue(a.getLastTrace().contains("retrieve"));
        assertTrue(log.get(0).systemPrompt.contains("Inheritance lets a subclass"));
    }

    @Test
    void ungroundedAnswerIsRegeneratedWithStrictInstruction() {
        int[] calls = {0};
        LearningAgent a = agent(fake(r -> "{}", r -> calls[0]++ == 0 ? "No citations at all." : "Subclasses reuse members [1]."));
        Answer ans = a.answerQuestion("s1", "sess", "c1", "What does inheritance let a subclass reuse?");
        assertEquals(2, log.size());
        assertTrue(log.get(1).systemPrompt.contains("IMPORTANT"));
        assertTrue(ans.grounded);
    }

    @Test
    void withoutLlmTheAnswerQuotesTheMaterial() {
        Answer ans = agent(new UnavailableLLMClient("none")).answerQuestion("s1", "sess", "c1", "explain polymorphism and overridden methods");
        assertTrue(ans.text.contains("Polymorphism means"));
        assertFalse(ans.citations.isEmpty());
    }

    @Test
    void unrelatedQuestionIsReportedAsNotCovered() {
        Answer ans = agent(fake(r -> "{}", r -> "should not be called")).answerQuestion("s1", "sess", "c1", "quantum chromodynamics gluon confinement");
        assertFalse(ans.grounded);
        assertTrue(ans.text.startsWith(LearningAgent.NOT_COVERED));
        assertTrue(log.isEmpty(), "no generation without grounding");
    }

    @Test
    void studyPlanPutsFocusTopicFirstWithoutLlm() {
        StudyPlanRequest req = new StudyPlanRequest("s1", "c1", LocalDate.now().plusDays(5), 60, List.of("Polymorphism"));
        StudyPlan plan = agent(new UnavailableLLMClient("none")).generateStudyPlan(req);
        assertEquals("Exam cram", plan.getStrategyName());
        assertEquals("Polymorphism", plan.getTasks().get(0).getConceptName());
        assertTrue(plan.getWarnings().stream().anyMatch(w -> w.contains("deterministic")));
    }

    @Test
    void llmPrioritiesReorderTopicsAndInventedNamesAreIgnored() {
        String json = "{\"topics\":[{\"name\":\"Made Up\",\"priority\":1},{\"name\":\"Polymorphism\",\"priority\":0.9,\"objective\":\"Explain dynamic dispatch\"}],"
                + "\"rationale\":\"Polymorphism first.\"}";
        StudyPlanRequest req = new StudyPlanRequest("s1", "c1", LocalDate.now().plusDays(30), 30, List.of());
        StudyPlan plan = agent(fake(r -> json, r -> "")).generateStudyPlan(req);
        assertEquals("Spaced repetition", plan.getStrategyName());
        assertEquals("Explain dynamic dispatch", plan.getTasks().get(0).getObjective());
        assertTrue(plan.getRationale().contains("Polymorphism first."));
        assertTrue(plan.getTasks().stream().anyMatch(t -> t.getConceptName().equals("Inheritance")), "omitted candidate kept");
    }

    @Test
    void adaptiveActivityUsesStateQuestionTypesAndAvoidsRecentConcept() {
        String json = "{\"questions\":[{\"type\":\"TRUE_FALSE\",\"prompt\":\"Polymorphism is resolved at runtime.\",\"answer\":\"true\","
                + "\"conceptName\":\"Polymorphism\",\"difficulty\":\"FOUNDATION\"}]}";
        LearningAgent a = agent(fake(r -> json, r -> ""));
        AdaptiveLearningSession s = new AdaptiveLearningSession("as1", "s1", "c1", new FoundationState());
        s.recordOutcome(new GradeResult("q0", "k1", true, 1.0, ""));
        Question q = a.nextAdaptiveActivity(s);
        assertEquals("k2", q.getConceptId());
        assertSame(q, s.getCurrentQuestion());
        assertTrue(log.get(0).userText().contains("TRUE_FALSE, MULTIPLE_CHOICE") || log.get(0).systemPrompt.contains("TRUE_FALSE, MULTIPLE_CHOICE"));
    }

    @Test
    void weakTopicsNeedEnoughEvidence() {
        WeakTopicReport early = agent(new UnavailableLLMClient("none")).diagnoseWeakTopics("s1", "c1");
        assertFalse(early.sufficientData);
        assertTrue(early.message.contains("Not enough graded practice"));

        ConceptMastery m = new ConceptMastery("s1", "c1", "k1", "Inheritance");
        for (int i = 0; i < 3; i++) m.applyResult(new GradeResult("q" + i, "k1", false, 0.1, ""));
        progress.saveMastery(m);
        WeakTopicReport report = agent(new UnavailableLLMClient("none")).diagnoseWeakTopics("s1", "c1");
        assertTrue(report.sufficientData);
        assertEquals("Inheritance", report.topics.get(0).conceptName);
        assertEquals(ActivityType.READ, report.topics.get(0).recommendedActivity);
    }
}
