package ca.yorku.eecs3311.studyagent.application;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.agent.ContextBuilder;
import ca.yorku.eecs3311.studyagent.agent.ResponseValidator;
import ca.yorku.eecs3311.studyagent.domain.*;
import ca.yorku.eecs3311.studyagent.integration.llm.*;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GradingAndProgressTest {
    private final SqliteDatabase db = SqliteDatabase.inMemory();
    private final SqliteProgressRepository progress = new SqliteProgressRepository(db);
    private final SqliteConceptRepository concepts = new SqliteConceptRepository(db);
    private final ProgressTracker tracker = new ProgressTracker(progress, concepts, new SqliteStudyPlanRepository(db));

    static LLMClient structured(String json) {
        return new LLMClient() {
            public LLMResponse generate(LLMRequest r) { return new LLMResponse("", 1, "STOP"); }
            public LLMResponse generateStructured(LLMRequest r, OutputSchema s) { return new LLMResponse(json, 1, "STOP"); }
            public String getModelName() { return "fake"; }
        };
    }

    private static Quiz quiz() {
        return new Quiz("qz1", "c1", DifficultyLevel.INTERMEDIATE, List.of(
                new TrueFalseQuestion("q1", "Java supports multiple class inheritance.", DifficultyLevel.FOUNDATION, "k1", "Inheritance", false),
                new MultipleChoiceQuestion("q2", "Which keyword creates a subclass?", DifficultyLevel.INTERMEDIATE, "k1", "Inheritance",
                        List.of("implements", "extends", "super"), 1),
                new ShortAnswerQuestion("q3", "Define polymorphism.", DifficultyLevel.ADVANCED, "k2", "Polymorphism",
                        "One interface, many implementations chosen at runtime", List.of("runtime method selection"))));
    }

    @Test
    void objectiveQuestionsAreGradedDeterministicallyAndMissingAnswersCountAsWrong() {
        GradingService grading = new GradingService(new UnavailableLLMClient("none"), new ContextBuilder(1000), new ResponseValidator());
        QuizAttempt a = grading.grade(quiz(), "s1", List.of(new QuestionResponse("q1", "false"), new QuestionResponse("q2", "B")));
        assertEquals(3, a.getResults().size());
        assertTrue(a.findResult("q1").orElseThrow().correct);
        assertTrue(a.findResult("q2").orElseThrow().correct);
        GradeResult q3 = a.findResult("q3").orElseThrow();
        assertFalse(q3.correct);
        assertEquals("", a.findResponse("q3").orElseThrow().answerText);
    }

    @Test
    void shortAnswerUsesLlmRubricGradeWhenValidAndKeywordFallbackOtherwise() {
        Question q3 = quiz().getQuestions().get(2);
        QuestionResponse r = new QuestionResponse("q3", "the method is selected at runtime");
        GradeResult ai = new GradingService(structured("{\"score\":0.9,\"feedback\":\"Good.\"}"), new ContextBuilder(1000), new ResponseValidator())
                .gradeResponse(q3, r);
        assertTrue(ai.correct);
        assertFalse(ai.needsReview, "AI-graded answers count toward mastery");

        GradeResult bad = new GradingService(structured("{\"score\":7,\"feedback\":\"?\"}"), new ContextBuilder(1000), new ResponseValidator())
                .gradeResponse(q3, r);
        assertTrue(bad.needsReview, "out-of-range score rejected, keyword grade kept");
    }

    @Test
    void trackerUpdatesMasteryAndNotifiesObservers() {
        List<ProgressEvent> seen = new ArrayList<>();
        tracker.attach(seen::add);
        GradingService grading = new GradingService(new UnavailableLLMClient("none"), new ContextBuilder(1000), new ResponseValidator());
        Quiz quiz = quiz();
        QuizAttempt a = grading.grade(quiz, "s1", List.of(new QuestionResponse("q1", "false"), new QuestionResponse("q2", "A"),
                new QuestionResponse("q3", "runtime method selection")));
        tracker.recordAttempt(a, quiz);

        assertEquals(2, seen.size(), "short answer needing review does not change mastery");
        ConceptMastery m = progress.findMastery("s1", "c1", "k1").orElseThrow();
        assertEquals(2, m.getAttempts());
        assertEquals(0.7, m.getMastery(), 1e-9, "first 1.0 then EMA with 0.0");
        assertEquals(1.0, seen.get(1).oldMastery);
        assertTrue(seen.get(1).isSharpDrop());
        assertEquals(3, progress.findPerformance("s1", "c1").size());
        assertEquals(1.0 * 2 / 3, tracker.getProgress("s1", "c1").quizAverage, 1e-9);
    }

    @Test
    void failingObserverDoesNotStopOthers() {
        List<ProgressEvent> seen = new ArrayList<>();
        tracker.attach(e -> { throw new IllegalStateException("boom"); });
        tracker.attach(seen::add);
        tracker.recordResult("s1", "c1", new GradeResult("q", "k9", true, 1.0, ""), "p", "a", DifficultyLevel.FOUNDATION, ActivityType.QUIZ);
        assertEquals(1, seen.size());
        assertEquals("k9", seen.get(0).conceptName, "unknown concept falls back to its id");
    }
}
