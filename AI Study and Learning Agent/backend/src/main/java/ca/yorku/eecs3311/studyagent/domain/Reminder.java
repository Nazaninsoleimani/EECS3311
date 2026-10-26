package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDateTime;

public class Reminder {
    private final String id;
    private final String studentId;
    private final String courseId;
    private final String message;
    private LocalDateTime dueAt;
    private final Recurrence recurrence;
    private ReminderStatus status = ReminderStatus.PENDING;

    public Reminder(String id, String studentId, String courseId, String message, LocalDateTime dueAt, Recurrence recurrence) {
        if (message == null || message.isBlank()) throw new ValidationException("Reminder message is required");
        if (dueAt == null) throw new ValidationException("Reminder time is required");
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.message = message.trim();
        this.dueAt = dueAt;
        this.recurrence = recurrence == null ? Recurrence.NONE : recurrence;
    }

    public boolean isDue(LocalDateTime now) {
        return status == ReminderStatus.PENDING && !dueAt.isAfter(now);
    }

    /** After delivery: one-off reminders become SENT, recurring ones move to their next time. */
    public void markDelivered(LocalDateTime now) {
        switch (recurrence) {
            case NONE -> status = ReminderStatus.SENT;
            case DAILY -> { while (!dueAt.isAfter(now)) dueAt = dueAt.plusDays(1); }
            case WEEKLY -> { while (!dueAt.isAfter(now)) dueAt = dueAt.plusWeeks(1); }
        }
    }

    public void cancel() { status = ReminderStatus.CANCELLED; }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public String getMessage() { return message; }
    public LocalDateTime getDueAt() { return dueAt; }
    public Recurrence getRecurrence() { return recurrence; }
    public ReminderStatus getStatus() { return status; }
}
