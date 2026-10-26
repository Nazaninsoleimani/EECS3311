package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDateTime;

public class LearningSession {
    protected final String id;
    protected final String studentId;
    protected final String courseId;
    protected final SessionType type;
    protected final LocalDateTime startedAt;
    protected LocalDateTime endedAt;

    public LearningSession(String id, String studentId, String courseId, SessionType type) {
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.type = type;
        this.startedAt = LocalDateTime.now();
    }

    public void end() { if (endedAt == null) endedAt = LocalDateTime.now(); }
    public boolean isActive() { return endedAt == null; }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public SessionType getType() { return type; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
}
