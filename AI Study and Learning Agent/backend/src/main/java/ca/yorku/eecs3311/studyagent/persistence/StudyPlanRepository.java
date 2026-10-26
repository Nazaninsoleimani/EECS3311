package ca.yorku.eecs3311.studyagent.persistence;

import ca.yorku.eecs3311.studyagent.domain.*;
import java.util.List;
import java.util.Optional;

public interface StudyPlanRepository {
    void save(StudyPlan plan);
    Optional<StudyPlan> findById(String id);
    Optional<StudyPlan> findActive(String studentId, String courseId);
    List<StudyPlan> findActiveByStudent(String studentId);
    Optional<StudyPlan> findByTask(String taskId);
    void deleteByCourse(String courseId);
}
