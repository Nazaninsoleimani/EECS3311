package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDateTime;

/** Exponential moving average of scores for one student and concept: m <- 0.7 m + 0.3 s. */
public class ConceptMastery {
    public static final double ALPHA = 0.3;
    public static final double WEAK_THRESHOLD = 0.5;
    public static final double MASTERED_THRESHOLD = 0.8;
    public static final int MIN_ATTEMPTS_FOR_WEAK = 3;

    private final String studentId;
    private final String courseId;
    private final String conceptId;
    private String conceptName;
    private double mastery;
    private int attempts;
    private LocalDateTime lastPracticed;

    public ConceptMastery(String studentId, String courseId, String conceptId, String conceptName) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.conceptId = conceptId;
        this.conceptName = conceptName;
    }

    /** The first result initialises mastery; later results are blended with weight ALPHA. */
    public void applyResult(GradeResult result) {
        if (result.needsReview) return;
        mastery = attempts == 0 ? result.score : (1 - ALPHA) * mastery + ALPHA * result.score;
        attempts++;
        lastPracticed = LocalDateTime.now();
    }

    public boolean isWeak() { return attempts >= MIN_ATTEMPTS_FOR_WEAK && mastery < WEAK_THRESHOLD; }
    public boolean isMastered() { return attempts > 0 && mastery >= MASTERED_THRESHOLD; }

    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public String getConceptId() { return conceptId; }
    public String getConceptName() { return conceptName; }
    public void setConceptName(String name) { this.conceptName = name; }
    public double getMastery() { return mastery; }
    public int getAttempts() { return attempts; }
    public LocalDateTime getLastPracticed() { return lastPracticed; }
    void setLastPracticed(LocalDateTime t) { this.lastPracticed = t; }
}
