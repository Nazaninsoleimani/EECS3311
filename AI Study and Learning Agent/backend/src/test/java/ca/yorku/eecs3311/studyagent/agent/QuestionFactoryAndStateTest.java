package ca.yorku.eecs3311.studyagent.agent;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.agent.tools.*;
import ca.yorku.eecs3311.studyagent.domain.*;
import ca.yorku.eecs3311.studyagent.integration.llm.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionFactoryAndStateTest {

    private static QuestionSpec spec(QuestionType type, String prompt, List<String> options, String answer) {
        QuestionSpec s = new QuestionSpec();
        s.type = type;
        s.prompt = prompt;
        s.options = options;
        s.answer = answer;
        s.conceptName = "Inheritance";
        return s;
    }

    @Test
    void factoriesCreateCorrectSubclasses() {
        Question mcq = new MultipleChoiceQuestionFactory().build(spec(QuestionType.MULTIPLE_CHOICE, "Pick",
                List.of("A) one", "B) two", "C) three", "D) four"), "C"));
        assertInstanceOf(MultipleChoiceQuestion.class, mcq);
        assertEquals(2, ((MultipleChoiceQuestion) mcq).getCorrectIndex());
        assertEquals("three", ((MultipleChoiceQuestion) mcq).getOptions().get(2), "letter prefixes stripped");
        assertInstanceOf(TrueFalseQuestion.class, new TrueFalseQuestionFactory().build(spec(QuestionType.TRUE_FALSE, "T?", null, "False")));
        assertInstanceOf(ShortAnswerQuestion.class, new ShortAnswerQuestionFactory().build(spec(QuestionType.SHORT_ANSWER, "Why?", null, "Because")));
    }

    @Test
    void factoriesRejectInvalidSpecs() {
        var mcqFactory = new MultipleChoiceQuestionFactory();
        assertThrows(ValidationException.class, () -> mcqFactory.build(spec(QuestionType.MULTIPLE_CHOICE, "Pick", List.of("a", "b"), "A")));
        assertThrows(ValidationException.class, () -> mcqFactory.build(spec(QuestionType.MULTIPLE_CHOICE, "Pick", List.of("a", "b", "c"), "Z")));
        assertThrows(ValidationException.class, () -> mcqFactory.build(spec(QuestionType.MULTIPLE_CHOICE, " ", List.of("a", "b", "c"), "A")));
        assertThrows(ValidationException.class, () -> new TrueFalseQuestionFactory().build(spec(QuestionType.TRUE_FALSE, "T?", null, "maybe")));
    }

    @Test
    void quizToolReplacesRejectedQuestionsInAnotherRound() {
        Deque<String> replies = new ArrayDeque<>(List.of(
                "{\"questions\":[{\"type\":\"MULTIPLE_CHOICE\",\"prompt\":\"Q1\",\"options\":[\"a\",\"b\",\"c\",\"d\"],\"answer\":\"B\",\"conceptName\":\"Inheritance\"},"
                        + "{\"type\":\"TRUE_FALSE\",\"prompt\":\"Q2\",\"answer\":\"perhaps\",\"conceptName\":\"Inheritance\"}]}",
                "{\"questions\":[{\"type\":\"TRUE_FALSE\",\"prompt\":\"Q3\",\"answer\":\"true\",\"conceptName\":\"inheritance\"}]}"));
        LLMClient fake = new LLMClient() {
            public LLMResponse generate(LLMRequest r) { throw new UnsupportedOperationException(); }
            public LLMResponse generateStructured(LLMRequest r, OutputSchema s) { return new LLMResponse(replies.pop(), 10, "STOP"); }
            public String getModelName() { return "fake"; }
        };
        QuizGenerationTool tool = new QuizGenerationTool(fake, new ContextBuilder(2000), new ResponseValidator());
        AgentContext ctx = new AgentContext(new LearnerContext("s", "c", new StudentProfile(), List.of(), List.of(), null));
        ToolResult r = tool.execute(ToolInput.of("context", ctx, "count", 2,
                "concepts", List.of(new Concept("k1", "c", "Inheritance", ""), new Concept("k2", "c", "Interfaces", ""))));
        assertTrue(r.success);
        List<Question> qs = r.payload();
        assertEquals(List.of("Q1", "Q3"), qs.stream().map(Question::getPrompt).toList());
        assertTrue(qs.stream().allMatch(q -> q.getConceptId().equals("k1")));
        assertTrue(r.message.contains("1 rejected"));
    }

    private static GradeResult res(boolean ok) {
        return new GradeResult("q", "k", ok, ok ? 1 : 0, "");
    }

    @Test
    void difficultyMovesUpAfterThreeCorrectAndDownAfterTwoIncorrect() {
        AdaptiveLearningSession s = new AdaptiveLearningSession("a", "s", "c", new FoundationState());
        s.recordOutcome(res(true));
        s.recordOutcome(res(true));
        assertEquals(DifficultyLevel.FOUNDATION, s.getLevel());
        s.recordOutcome(res(true));
        assertEquals(DifficultyLevel.INTERMEDIATE, s.getLevel());
        assertEquals(0, s.getStreak());
        s.recordOutcome(res(false));
        s.recordOutcome(res(true));
        s.recordOutcome(res(false));
        assertEquals(DifficultyLevel.INTERMEDIATE, s.getLevel(), "non-consecutive misses do not demote");
        s.recordOutcome(res(false));
        assertEquals(DifficultyLevel.FOUNDATION, s.getLevel());
        assertEquals(List.of("FOUNDATION", "INTERMEDIATE", "FOUNDATION"), s.getLevelHistory());
    }

    @Test
    void foundationNeverDemotesAndAdvancedNeverPromotes() {
        AdaptiveLearningSession low = new AdaptiveLearningSession("a", "s", "c", new FoundationState());
        for (int i = 0; i < 4; i++) low.recordOutcome(res(false));
        assertEquals(DifficultyLevel.FOUNDATION, low.getLevel());
        AdaptiveLearningSession high = new AdaptiveLearningSession("b", "s", "c", new AdvancedState());
        for (int i = 0; i < 5; i++) high.recordOutcome(res(true));
        assertEquals(DifficultyLevel.ADVANCED, high.getLevel());
        assertEquals(List.of(QuestionType.SHORT_ANSWER, QuestionType.MULTIPLE_CHOICE), high.getState().preferredQuestionTypes());
    }
}
