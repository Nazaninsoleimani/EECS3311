package ca.yorku.eecs3311.studyagent.agent;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.agent.tools.ToolResult;
import ca.yorku.eecs3311.studyagent.domain.DocumentChunk;
import ca.yorku.eecs3311.studyagent.domain.StudentProfile;
import ca.yorku.eecs3311.studyagent.integration.llm.*;
import ca.yorku.eecs3311.studyagent.integration.retrieval.RetrievedChunk;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentCoreTest {
    private final ResponseValidator validator = new ResponseValidator();

    private static RetrievedChunk chunk(String text, double score) {
        return new RetrievedChunk(new DocumentChunk("id" + score, "m", "c", "notes.pdf", 0, text, "p.1"), score);
    }

    private static AgentContext ctx() {
        return new AgentContext(new LearnerContext("s1", "c1", new StudentProfile(), List.of(), List.of(), null));
    }

    @Test
    void validatorExtractsFencedJsonAndChecksFields() {
        OutputSchema schema = new OutputSchema("s", "{}", List.of("summary", "keyPoints"));
        ValidationResult ok = validator.validate(new LLMResponse("```json\n{\"summary\":\"x\",\"keyPoints\":[\"a\"]}\n```", 0, ""), schema);
        assertTrue(ok.valid);
        ValidationResult missing = validator.validate(new LLMResponse("{\"summary\":\"x\",\"keyPoints\":[]}", 0, ""), schema);
        assertFalse(missing.valid);
        assertFalse(validator.validate(new LLMResponse("no json here", 0, ""), schema).valid);
    }

    @Test
    void groundingRequiresValidCitations() {
        List<RetrievedChunk> sources = List.of(chunk("a", 0.9), chunk("b", 0.8));
        assertTrue(validator.checkGrounding("Subclasses inherit fields [1][2].", sources));
        assertFalse(validator.checkGrounding("No citations here.", sources));
        assertFalse(validator.checkGrounding("Made up [3].", sources));
    }

    @Test
    void contextBuilderFillsTemplateAndTrimsToBudget() {
        AgentContext c = ctx().set("question", "What is polymorphism?").set("history", "").set("strict", "");
        c.retrieved.add(chunk("x".repeat(400), 0.5));
        c.retrieved.add(chunk("y".repeat(400), 0.9));
        LLMRequest req = new ContextBuilder(150).build(PromptTask.ANSWER_WITH_CITATIONS, c);
        assertTrue(req.systemPrompt.contains("[1] (notes.pdf p.1)"));
        assertTrue(req.systemPrompt.contains("yyyy"), "highest score kept");
        assertFalse(req.systemPrompt.contains("xxxx"), "over budget dropped");
        assertTrue(req.userText().contains("What is polymorphism?"));
        assertFalse(req.userText().contains("{{"));
    }

    @Test
    void plannerSkipsGenerationWhenNothingRetrieved() {
        Planner planner = new Planner(new UnavailableLLMClient("none"), new ContextBuilder(1000));
        AgentGoal goal = new AgentGoal(GoalType.GENERATE_FLASHCARDS, "cards").with("courseId", "c1").with("topic", "x");
        AgentPlan plan = planner.createPlan(goal, ctx());
        PlanStep retrieve = plan.nextStep();
        assertEquals("retrieval", retrieve.toolName);
        retrieve.status = StepStatus.DONE;
        planner.revise(plan, retrieve, ToolResult.ok(List.of()));
        assertTrue(plan.isComplete());
        assertTrue(plan.hasNote("no-grounding"));
    }

    @Test
    void plannerChoosesMapReduceForLongMaterial() {
        Planner planner = new Planner(new UnavailableLLMClient("none"), new ContextBuilder(1000));
        AgentPlan small = planner.createPlan(new AgentGoal(GoalType.SUMMARIZE, "").with("totalTokens", 1000), ctx());
        AgentPlan large = planner.createPlan(new AgentGoal(GoalType.SUMMARIZE, "").with("totalTokens", 20000), ctx());
        assertTrue(small.hasNote("mode=single-pass"));
        assertTrue(large.hasNote("mode=map-reduce"));
    }

    @Test
    void followUpQueryFallsBackToPreviousQuestionWithoutLlm() {
        Planner planner = new Planner(new UnavailableLLMClient("none"), new ContextBuilder(1000));
        String q = planner.retrievalQuery("why is that?", "Student: What is dynamic dispatch?\nTutor: ...", ctx());
        assertEquals("What is dynamic dispatch? why is that?", q);
        assertEquals("Explain the difference between overloading and overriding in Java classes please",
                planner.retrievalQuery("Explain the difference between overloading and overriding in Java classes please", "Student: hi", ctx()));
    }

    @Test
    void sessionMemoryKeepsLastTurns() {
        SessionMemory m = new SessionMemory("s", 2);
        m.add(new ConversationTurn("q1", "a1"));
        m.add(new ConversationTurn("q2", "a2"));
        m.add(new ConversationTurn("q3", "a3"));
        assertEquals(List.of("q2", "q3"), m.recent(5).stream().map(t -> t.question).toList());
    }
}
