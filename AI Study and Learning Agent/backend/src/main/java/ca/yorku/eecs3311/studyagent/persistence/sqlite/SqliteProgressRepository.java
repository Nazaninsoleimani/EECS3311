package ca.yorku.eecs3311.studyagent.persistence.sqlite;

import ca.yorku.eecs3311.studyagent.domain.ConceptMastery;
import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.PerformanceRecord;
import ca.yorku.eecs3311.studyagent.persistence.ProgressRepository;
import java.util.List;
import java.util.Optional;

public class SqliteProgressRepository implements ProgressRepository {
    private final SqliteDatabase db;

    public SqliteProgressRepository(SqliteDatabase db) { this.db = db; }

    private static String masteryId(String studentId, String conceptId) { return studentId + "|" + conceptId; }

    @Override
    public List<ConceptMastery> findMastery(String studentId, String courseId) {
        return db.byParent("mastery", courseId, ConceptMastery.class).stream()
                .filter(m -> m.getStudentId().equals(studentId)).toList();
    }

    @Override
    public Optional<ConceptMastery> findMastery(String studentId, String courseId, String conceptId) {
        return db.get("mastery", masteryId(studentId, conceptId), ConceptMastery.class);
    }

    @Override
    public void saveMastery(ConceptMastery m) {
        db.put("mastery", masteryId(m.getStudentId(), m.getConceptId()), m.getCourseId(), m.getStudentId(), m);
    }

    @Override
    public void savePerformance(PerformanceRecord r) {
        db.put("performance", Ids.newId("pr"), r.courseId, r.studentId, r);
    }

    @Override
    public List<PerformanceRecord> findPerformance(String studentId, String courseId) {
        return db.byParent("performance", courseId, PerformanceRecord.class).stream()
                .filter(r -> r.studentId.equals(studentId)).toList();
    }

    @Override
    public void deleteByCourse(String courseId) {
        db.deleteByParent("mastery", courseId);
        db.deleteByParent("performance", courseId);
    }
}
