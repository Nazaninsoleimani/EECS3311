package ca.yorku.eecs3311.studyagent.domain;

public class TopicPriority {
    public final Concept concept;
    public final double priority;
    public String reason;
    public String objective;

    public TopicPriority(Concept concept, double priority, String reason) {
        this.concept = concept;
        this.priority = priority;
        this.reason = reason;
    }
}
