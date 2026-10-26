package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudyPlan {
    private final String id;
    private final String studentId;
    private final String courseId;
    private final LocalDateTime createdAt;
    private final String strategyName;
    private String rationale;
    private PlanStatus status = PlanStatus.ACTIVE;
    private final List<StudyTask> tasks;
    private final List<String> unscheduledTopics = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private int revision;

    public StudyPlan(String id, String studentId, String courseId, String strategyName, String rationale, List<StudyTask> tasks) {
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.createdAt = LocalDateTime.now();
        this.strategyName = strategyName;
        this.rationale = rationale;
        this.tasks = new ArrayList<>(tasks);
    }

    public double completionRate() {
        if (tasks.isEmpty()) return 0.0;
        return (double) tasks.stream().filter(StudyTask::isDone).count() / tasks.size();
    }

    public List<StudyTask> tasksFor(LocalDate date) {
        return tasks.stream().filter(t -> t.getDate().equals(date)).toList();
    }

    public Optional<StudyTask> findTask(String taskId) {
        return tasks.stream().filter(t -> t.getId().equals(taskId)).findFirst();
    }

    public boolean coversConcept(String conceptId) {
        return tasks.stream().anyMatch(t -> conceptId.equals(t.getConceptId()) && !t.isDone());
    }

    public void archive() { status = PlanStatus.ARCHIVED; }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getStrategyName() { return strategyName; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public PlanStatus getStatus() { return status; }
    public List<StudyTask> getTasks() { return tasks; }
    public List<String> getUnscheduledTopics() { return unscheduledTopics; }
    public List<String> getWarnings() { return warnings; }
    public int getRevision() { return revision; }
    public void setRevision(int revision) { this.revision = revision; }
}
