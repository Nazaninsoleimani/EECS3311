package ca.yorku.eecs3311.studyagent.domain;

public class WeakTopic {
    public final String conceptId;
    public final String conceptName;
    public final double mastery;
    public final double priority;
    public String diagnosis;
    public ActivityType recommendedActivity;

    public WeakTopic(String conceptId, String conceptName, double mastery, double priority,
                     String diagnosis, ActivityType recommendedActivity) {
        this.conceptId = conceptId;
        this.conceptName = conceptName;
        this.mastery = mastery;
        this.priority = priority;
        this.diagnosis = diagnosis;
        this.recommendedActivity = recommendedActivity;
    }
}
