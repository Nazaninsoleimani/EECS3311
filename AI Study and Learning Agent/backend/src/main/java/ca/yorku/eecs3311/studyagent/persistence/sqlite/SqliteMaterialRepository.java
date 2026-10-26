package ca.yorku.eecs3311.studyagent.persistence.sqlite;

import ca.yorku.eecs3311.studyagent.domain.CourseMaterial;
import ca.yorku.eecs3311.studyagent.domain.DocumentChunk;
import ca.yorku.eecs3311.studyagent.domain.Summary;
import ca.yorku.eecs3311.studyagent.persistence.MaterialRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SqliteMaterialRepository implements MaterialRepository {
    private final SqliteDatabase db;

    public SqliteMaterialRepository(SqliteDatabase db) { this.db = db; }

    @Override public void save(CourseMaterial m) { db.put("material", m.getId(), m.getCourseId(), null, m); }
    @Override public Optional<CourseMaterial> findById(String id) { return db.get("material", id, CourseMaterial.class); }
    @Override public List<CourseMaterial> findByCourse(String courseId) { return db.byParent("material", courseId, CourseMaterial.class); }

    @Override
    public void saveChunks(String materialId, List<DocumentChunk> chunks) {
        db.deleteByParent("chunk", materialId);
        for (DocumentChunk c : chunks) db.put("chunk", c.getId(), materialId, c.getCourseId(), c);
    }

    @Override
    public List<DocumentChunk> findChunks(String materialId) {
        return db.byParent("chunk", materialId, DocumentChunk.class).stream()
                .sorted(Comparator.comparingInt(DocumentChunk::getIndex)).toList();
    }

    @Override public List<DocumentChunk> findChunksByCourse(String courseId) { return db.byOwner("chunk", courseId, DocumentChunk.class); }

    @Override
    public void saveSummary(String materialId, Summary summary) {
        findById(materialId).ifPresent(m -> {
            m.setSummary(summary);
            save(m);
        });
    }

    @Override
    public void delete(String id) {
        db.deleteByParent("chunk", id);
        db.delete("material", id);
    }
}
