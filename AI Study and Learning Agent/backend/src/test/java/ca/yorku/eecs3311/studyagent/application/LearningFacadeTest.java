package ca.yorku.eecs3311.studyagent.application;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.config.AppConfig;
import ca.yorku.eecs3311.studyagent.config.AppContext;
import ca.yorku.eecs3311.studyagent.domain.*;
import ca.yorku.eecs3311.studyagent.integration.llm.*;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteDatabase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** End-to-end through the facade with a scripted LLM that answers per output schema. */
class LearningFacadeTest {
    @TempDir Path dir;

    static final String CONCEPTS = "{\"concepts\":[{\"name\":\"Observer Pattern\",\"definition\":\"subjects notify observers\"},"
            + "{\"name\":\"Adapter Pattern\",\"definition\":\"converts an interface\"}],"
            + "\"relations\":[{\"from\":\"Adapter Pattern\",\"to\":\"Observer Pattern\",\"type\":\"RELATED_TO\"}]}";
    static final String QUIZ = "{\"questions\":["
            + "{\"type\":\"TRUE_FALSE\",\"prompt\":\"Observers are notified by the subject.\",\"answer\":\"true\",\"conceptName\":\"Observer Pattern\"},"
            + "{\"type\":\"MULTIPLE_CHOICE\",\"prompt\":\"What does an adapter convert?\",\"options\":[\"An interface\",\"A database\",\"A thread\"],"
            + "\"answer\":\"A\",\"conceptName\":\"Adapter Pattern\"}]}";
    static final String CARDS = "{\"cards\":[{\"front\":\"What does a subject do?\",\"back\":\"Notifies observers\",\"conceptName\":\"Observer Pattern\"}]}";

    static LLMClient scripted() {
        Map<String, String> bySchema = Map.of("concepts", CONCEPTS, "quiz", QUIZ, "flashcards", CARDS,
                "summary", "{\"summary\":\"Two patterns.\",\"keyPoints\":[\"Observer\",\"Adapter\"]}",
                "priorities", "{\"topics\":[{\"name\":\"Adapter Pattern\",\"priority\":0.9}],\"rationale\":\"Start with the weakest pattern.\"}");
        return new LLMClient() {
            public LLMResponse generate(LLMRequest r) { return new LLMResponse("Subjects notify their observers [1].", 10, "STOP"); }
            public LLMResponse generateStructured(LLMRequest r, OutputSchema s) { return new LLMResponse(bySchema.getOrDefault(s.name, "{}"), 10, "STOP"); }
            public String getModelName() { return "scripted"; }
        };
    }

    @Test
    void fullStudyWorkflowRunsThroughTheFacade() throws Exception {
        List<String> notifications = new ArrayList<>();
        try (AppContext app = AppContext.create(new AppConfig(Map.of()), SqliteDatabase.inMemory(), scripted(),
                (t, b, to) -> notifications.add(b))) {
            LearningFacade f = app.facade();
            String me = app.studentId();
            Course c = f.createCourse(me, "EECS3311", "Software Design", LocalDate.now().plusDays(10));
            Path notes = Files.writeString(dir.resolve("patterns.txt"),
                    "The observer pattern: a subject notifies its observers when its state changes. ".repeat(20)
                            + "The adapter pattern converts the interface of a class into another interface clients expect. ".repeat(20));
            CourseMaterial m = f.uploadMaterial(c.getId(), notes);
            assertTrue(m.isIndexed());

            assertEquals("Two patterns.", f.summarizeMaterial(m.getId(), new SummaryOptions(SummaryLength.BRIEF)).getText());
            ConceptGraph graph = f.extractConcepts(m.getId());
            assertEquals(2, graph.nodes.size());
            assertEquals(1, graph.edges.size());

            Answer a = f.askQuestion(f.newSessionId(), c.getId(), "What does a subject do in the observer pattern?");
            assertTrue(a.grounded);

            StudyPlan plan = f.generateStudyPlan(new StudyPlanRequest(me, c.getId(), c.getExamDate(), 60, List.of()));
            assertEquals("Balanced review", plan.getStrategyName());
            assertTrue(plan.getRationale().contains("weakest pattern"));
            f.completeStudyTask(plan.getTasks().get(0).getId());
            assertTrue(f.getActivePlan(me, c.getId()).getTasks().get(0).isDone());

            Quiz quiz = f.generateQuiz(new QuizRequest(me, c.getId(), List.of(), 2, null, null));
            assertEquals(2, quiz.getQuestions().size());
            QuizAttempt attempt = f.submitQuiz(quiz.getId(), List.of(
                    new QuestionResponse(quiz.getQuestions().get(0).getId(), "true"),
                    new QuestionResponse(quiz.getQuestions().get(1).getId(), "B")));
            assertEquals(0.5, attempt.computeScore(), 1e-9);
            assertTrue(f.explainAnswer(attempt.getId(), quiz.getQuestions().get(1).getId()).text.contains("[1]"));
            assertEquals(2, f.getProgress(me, c.getId()).masteries.size());

            FlashcardDeck deck = f.generateFlashcards(new FlashcardRequest(me, c.getId(), List.of(), "observer", 1));
            assertEquals(1, f.dueFlashcards(c.getId()).size());
            f.reviewFlashcard(deck.getCards().get(0).getId(), ReviewRating.GOOD);
            assertTrue(f.dueFlashcards(c.getId()).isEmpty());

            AdaptiveLearningSession s = f.startAdaptiveSession(me, c.getId());
            assertNotNull(s.getCurrentQuestion());
            ActivityOutcome out = f.submitActivityResult(s.getId(), new QuestionResponse(s.getCurrentQuestion().getId(), "true"));
            assertNotNull(out.result());
            assertEquals(1, f.endAdaptiveSession(s.getId()).getAnswered());

            assertTrue(ConceptMapService.renderText(f.getConceptMap(me, c.getId())).contains("Observer Pattern"));
            List<EventType> types = f.getHistory(me, HistoryFilter.all()).stream().map(LearningEvent::getType).toList();
            assertTrue(types.containsAll(List.of(EventType.COURSE_CREATED, EventType.MATERIAL_UPLOADED, EventType.CONCEPTS_EXTRACTED,
                    EventType.QUESTION_ASKED, EventType.PLAN_GENERATED, EventType.QUIZ_COMPLETED, EventType.FLASHCARD_REVIEWED,
                    EventType.SESSION_ENDED)));
            assertTrue(f.status().contains("scripted"));

            f.deleteCourse(c.getId());
            assertTrue(f.listCourses(me).isEmpty());
        }
    }

    @Test
    void withoutApiKeyFeaturesUseFallbacksOrFailClearly() throws Exception {
        try (AppContext app = AppContext.create(new AppConfig(Map.of()), SqliteDatabase.inMemory(),
                new UnavailableLLMClient("no key"), (t, b, to) -> true)) {
            LearningFacade f = app.facade();
            Course c = f.createCourse(app.studentId(), "EECS3311", "Software Design", null);
            CourseMaterial m = f.uploadMaterial(c.getId(), Files.writeString(dir.resolve("n.txt"), "Factory method defers creation to subclasses. ".repeat(30)));
            assertTrue(f.askQuestion("s", c.getId(), "what does factory method defer to subclasses?").text.contains("unavailable"));
            Exception e = assertThrows(RuntimeException.class, () -> f.summarizeMaterial(m.getId(), new SummaryOptions(null)));
            assertTrue(e.getMessage().contains("Summary unavailable"));
            assertTrue(f.status().contains("not configured"));
        }
    }
}
