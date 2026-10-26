package ca.yorku.eecs3311.studyagent.domain;

import java.util.Locale;
import java.util.Set;

public class TrueFalseQuestion extends Question {
    private static final Set<String> TRUE_WORDS = Set.of("true", "t", "yes", "y");
    private static final Set<String> FALSE_WORDS = Set.of("false", "f", "no", "n");

    private final boolean correctValue;

    public TrueFalseQuestion(String id, String prompt, DifficultyLevel difficulty, String conceptId,
                             String conceptName, boolean correctValue) {
        super(id, prompt, difficulty, conceptId, conceptName);
        this.correctValue = correctValue;
    }

    @Override
    public GradeResult grade(QuestionResponse response) {
        if (isBlank(response)) return unanswered();
        String a = response.answerText.trim().toLowerCase(Locale.ROOT);
        Boolean given = TRUE_WORDS.contains(a) ? Boolean.TRUE : FALSE_WORDS.contains(a) ? Boolean.FALSE : null;
        boolean correct = given != null && given == correctValue;
        return new GradeResult(id, conceptId, correct, correct ? 1.0 : 0.0,
                correct ? "Correct." : "Incorrect. The statement is " + correctAnswerText() + ".");
    }

    @Override public boolean isObjective() { return true; }
    @Override public QuestionType getType() { return QuestionType.TRUE_FALSE; }
    @Override public String correctAnswerText() { return correctValue ? "true" : "false"; }

    public boolean getCorrectValue() { return correctValue; }
}
