package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WeakTopicReport {
    public final String courseId;
    public final boolean sufficientData;
    public final LocalDateTime generatedAt = LocalDateTime.now();
    public final List<WeakTopic> topics = new ArrayList<>();
    /** Explains fallbacks, e.g. "not enough data" or "diagnosis unavailable". */
    public String message;
    public boolean aiDiagnosed;

    public WeakTopicReport(String courseId, boolean sufficientData) {
        this.courseId = courseId;
        this.sufficientData = sufficientData;
    }
}
