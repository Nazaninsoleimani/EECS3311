package ca.yorku.eecs3311.studyagent.domain;

public class GradeResult {
    public final String questionId;
    public final String conceptId;
    public final boolean correct;
    public final double score;
    public final String feedback;
    /** True when the score is not trustworthy (LLM unavailable); excluded from mastery updates. */
    public boolean needsReview;

    public GradeResult(String questionId, String conceptId, boolean correct, double score, String feedback) {
        this.questionId = questionId;
        this.conceptId = conceptId;
        this.correct = correct;
        this.score = Math.max(0.0, Math.min(1.0, score));
        this.feedback = feedback;
    }
}
