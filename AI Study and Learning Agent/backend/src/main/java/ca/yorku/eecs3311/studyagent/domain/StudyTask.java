package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDate;

public class StudyTask {
    private final String id;
    private final LocalDate date;
    private final int durationMinutes;
    private final ActivityType activity;
    private final String conceptId;
    private final String conceptName;
    private String objective;
    private TaskStatus status = TaskStatus.PENDING;

    public StudyTask(String id, LocalDate date, int durationMinutes, ActivityType activity,
                     String conceptId, String conceptName, String objective) {
        this.id = id;
        this.date = date;
        this.durationMinutes = durationMinutes;
        this.activity = activity;
        this.conceptId = conceptId;
        this.conceptName = conceptName;
        this.objective = objective;
    }

    public void complete() { status = TaskStatus.DONE; }
    public boolean isDone() { return status == TaskStatus.DONE; }

    public String getId() { return id; }
    public LocalDate getDate() { return date; }
    public int getDurationMinutes() { return durationMinutes; }
    public ActivityType getActivity() { return activity; }
    public String getConceptId() { return conceptId; }
    public String getConceptName() { return conceptName; }
    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }
    public TaskStatus getStatus() { return status; }
}
