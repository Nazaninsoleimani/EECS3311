package ca.yorku.eecs3311.studyagent.persistence.sqlite;

import ca.yorku.eecs3311.studyagent.domain.PlanStatus;
import ca.yorku.eecs3311.studyagent.domain.StudyPlan;
import ca.yorku.eecs3311.studyagent.persistence.StudyPlanRepository;
import java.util.List;
import java.util.Optional;

public class SqliteStudyPlanRepository implements StudyPlanRepository {
    private final SqliteDatabase db;

    public SqliteStudyPlanRepository(SqliteDatabase db) { this.db = db; }

    @Override public void save(StudyPlan plan) { db.put("plan", plan.getId(), plan.getCourseId(), plan.getStudentId(), plan); }
    @Override public Optional<StudyPlan> findById(String id) { return db.get("plan", id, StudyPlan.class); }

    @Override
    public Optional<StudyPlan> findActive(String studentId, String courseId) {
        return db.byParent("plan", courseId, StudyPlan.class).stream()
                .filter(p -> p.getStudentId().equals(studentId) && p.getStatus() == PlanStatus.ACTIVE)
                .reduce((first, second) -> second);
    }

    @Override
    public List<StudyPlan> findActiveByStudent(String studentId) {
        return db.byOwner("plan", studentId, StudyPlan.class).stream()
                .filter(p -> p.getStatus() == PlanStatus.ACTIVE).toList();
    }

    @Override
    public Optional<StudyPlan> findByTask(String taskId) {
        return db.all("plan", StudyPlan.class).stream()
                .filter(p -> p.findTask(taskId).isPresent()).findFirst();
    }

    @Override public void deleteByCourse(String courseId) { db.deleteByParent("plan", courseId); }
}
