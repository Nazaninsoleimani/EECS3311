package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDateTime;

public class LearningEvent {
    private final String id;
    private final String studentId;
    private final String courseId;
    private final EventType type;
    private final LocalDateTime timestamp;
    private final String summary;
    private final String refId;

    public LearningEvent(String studentId, String courseId, EventType type, String summary, String refId) {
        this(Ids.newId("ev"), studentId, courseId, type, LocalDateTime.now(), summary, refId);
    }

    public LearningEvent(String id, String studentId, String courseId, EventType type, LocalDateTime timestamp,
                         String summary, String refId) {
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.type = type;
        this.timestamp = timestamp;
        this.summary = summary;
        this.refId = refId;
    }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public EventType getType() { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getSummary() { return summary; }
    public String getRefId() { return refId; }
}
