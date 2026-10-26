package ca.yorku.eecs3311.studyagent.persistence;

import ca.yorku.eecs3311.studyagent.domain.*;
import java.util.List;
import java.util.Optional;

public interface MaterialRepository {
    void save(CourseMaterial m);
    Optional<CourseMaterial> findById(String id);
    List<CourseMaterial> findByCourse(String courseId);
    void saveChunks(String materialId, List<DocumentChunk> chunks);
    List<DocumentChunk> findChunks(String materialId);
    List<DocumentChunk> findChunksByCourse(String courseId);
    void saveSummary(String materialId, Summary summary);
    void delete(String id);
}
