package ca.yorku.eecs3311.studyagent.domain;

/**
 * Product of the Factory Method pattern (created by QuestionFactory subclasses).
 * Each subclass grades its own responses, so GradingService never switches on type.
 */
public abstract class Question {
    protected final String id;
    protected final String prompt;
    protected final DifficultyLevel difficulty;
    protected final String conceptId;
    protected final String conceptName;

    protected Question(String id, String prompt, DifficultyLevel difficulty, String conceptId, String conceptName) {
        if (prompt == null || prompt.isBlank()) throw new ValidationException("Question prompt is empty");
        this.id = id;
        this.prompt = prompt.trim();
        this.difficulty = difficulty == null ? DifficultyLevel.FOUNDATION : difficulty;
        this.conceptId = conceptId;
        this.conceptName = conceptName;
    }

    public abstract GradeResult grade(QuestionResponse response);

    /** Objective questions are graded exactly; others need rubric scoring. */
    public abstract boolean isObjective();

    public abstract QuestionType getType();

    /** The correct answer as text, shown in results and explanations. */
    public abstract String correctAnswerText();

    protected GradeResult unanswered() {
        return new GradeResult(id, conceptId, false, 0.0, "No answer given. Correct answer: " + correctAnswerText());
    }

    protected static boolean isBlank(QuestionResponse r) {
        return r == null || r.answerText == null || r.answerText.isBlank();
    }

    public String getId() { return id; }
    public String getPrompt() { return prompt; }
    public DifficultyLevel getDifficulty() { return difficulty; }
    public String getConceptId() { return conceptId; }
    public String getConceptName() { return conceptName; }
}
