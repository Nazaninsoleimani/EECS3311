package ca.yorku.eecs3311.studyagent.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionGradingTest {

    private final MultipleChoiceQuestion mcq = new MultipleChoiceQuestion("q1", "Which keyword creates a subclass?",
            DifficultyLevel.FOUNDATION, "k1", "Inheritance", List.of("implements", "extends", "super"), 1);

    @Test
    void mcqAcceptsLetterNumberOrText() {
        assertTrue(mcq.grade(new QuestionResponse("q1", "b")).correct);
        assertTrue(mcq.grade(new QuestionResponse("q1", "2")).correct);
        assertTrue(mcq.grade(new QuestionResponse("q1", "Extends")).correct);
        assertFalse(mcq.grade(new QuestionResponse("q1", "A")).correct);
        assertFalse(mcq.grade(new QuestionResponse("q1", "Z")).correct);
    }

    @Test
    void unansweredScoresZero() {
        GradeResult r = mcq.grade(new QuestionResponse("q1", "  "));
        assertFalse(r.correct);
        assertEquals(0.0, r.score);
    }

    @Test
    void mcqRejectsBadCorrectIndex() {
        assertThrows(ValidationException.class, () -> new MultipleChoiceQuestion("q", "p", null, null, null,
                List.of("a", "b"), 2));
    }

    @Test
    void trueFalseAcceptsSynonyms() {
        TrueFalseQuestion tf = new TrueFalseQuestion("q2", "Java supports multiple class inheritance.",
                DifficultyLevel.FOUNDATION, "k1", "Inheritance", false);
        assertTrue(tf.grade(new QuestionResponse("q2", "F")).correct);
        assertTrue(tf.grade(new QuestionResponse("q2", "no")).correct);
        assertFalse(tf.grade(new QuestionResponse("q2", "true")).correct);
        assertFalse(tf.grade(new QuestionResponse("q2", "maybe")).correct);
    }

    @Test
    void shortAnswerFallbackUsesRubricAndNeedsReview() {
        ShortAnswerQuestion sa = new ShortAnswerQuestion("q3", "What is dynamic dispatch?", DifficultyLevel.ADVANCED,
                "k2", "Polymorphism", "The method body is chosen at runtime from the object's dynamic type",
                List.of("method chosen at runtime", "based on dynamic type of object"));
        GradeResult r = sa.grade(new QuestionResponse("q3", "The runtime picks the method using the object's dynamic type"));
        assertTrue(r.needsReview);
        assertEquals(1.0, r.score);
        assertFalse(sa.isObjective());
    }

    @Test
    void attemptScoreIsMeanOfResults() {
        QuizAttempt a = new QuizAttempt("a1", "z1", "s1", "c1", List.of(), List.of(
                new GradeResult("q1", "k1", true, 1.0, ""),
                new GradeResult("q2", "k1", false, 0.0, ""),
                new GradeResult("q3", "k2", false, 0.5, "")));
        assertEquals(0.5, a.computeScore(), 1e-9);
        assertEquals(1, a.correctCount());
    }
}
