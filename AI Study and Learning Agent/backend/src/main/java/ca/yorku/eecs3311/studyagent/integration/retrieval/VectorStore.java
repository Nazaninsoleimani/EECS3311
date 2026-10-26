package ca.yorku.eecs3311.studyagent.integration.retrieval;

import ca.yorku.eecs3311.studyagent.domain.DocumentChunk;
import java.util.List;

public interface VectorStore {
    void upsert(List<DocumentChunk> chunks, List<float[]> vectors, String model);

    /** Top-k chunks of the course by cosine similarity, considering only vectors of the same model. */
    List<RetrievedChunk> search(float[] vector, int k, String courseId, String model);

    void deleteByMaterial(String materialId);

    void deleteByCourse(String courseId);
}
