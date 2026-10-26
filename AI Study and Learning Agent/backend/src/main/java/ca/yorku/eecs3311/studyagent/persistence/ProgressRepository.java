package ca.yorku.eecs3311.studyagent.persistence;

import ca.yorku.eecs3311.studyagent.domain.*;
import java.util.List;
import java.util.Optional;

public interface ProgressRepository {
    List<ConceptMastery> findMastery(String studentId, String courseId);
    Optional<ConceptMastery> findMastery(String studentId, String courseId, String conceptId);
    void saveMastery(ConceptMastery mastery);
    void savePerformance(PerformanceRecord record);
    List<PerformanceRecord> findPerformance(String studentId, String courseId);
    void deleteByCourse(String courseId);
}
