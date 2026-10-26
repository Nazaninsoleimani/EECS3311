package ca.yorku.eecs3311.studyagent.domain;

/** Published by ProgressTracker to its observers whenever a concept's mastery changes. */
public class ProgressEvent {
    public static final double SHARP_DROP = 0.15;

    public final String studentId;
    public final String courseId;
    public final String conceptId;
    public final String conceptName;
    public final double oldMastery;
    public final double newMastery;

    public ProgressEvent(String studentId, String courseId, String conceptId, String conceptName,
                         double oldMastery, double newMastery) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.conceptId = conceptId;
        this.conceptName = conceptName;
        this.oldMastery = oldMastery;
        this.newMastery = newMastery;
    }

    public boolean isSharpDrop() { return oldMastery - newMastery >= SHARP_DROP; }
    public boolean fellBelowWeakThreshold() {
        return oldMastery >= ConceptMastery.WEAK_THRESHOLD && newMastery < ConceptMastery.WEAK_THRESHOLD;
    }
}
