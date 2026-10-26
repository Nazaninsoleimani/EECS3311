package ca.yorku.eecs3311.studyagent.integration.retrieval;

import ca.yorku.eecs3311.studyagent.domain.CourseMaterial;
import ca.yorku.eecs3311.studyagent.domain.DocumentChunk;
import java.util.List;

/** Embeds and indexes chunks, and retrieves the relevant ones above a similarity threshold. */
public class RetrievalService {
    private final EmbeddingService embeddings;
    private final VectorStore store;
    private final double minScore;

    public RetrievalService(EmbeddingService embeddings, VectorStore store) {
        this(embeddings, store, embeddings.suggestedMinScore());
    }

    public RetrievalService(EmbeddingService embeddings, VectorStore store, double minScore) {
        this.embeddings = embeddings;
        this.store = store;
        this.minScore = minScore;
    }

    public void index(CourseMaterial material, List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) return;
        List<float[]> vectors = embeddings.embed(chunks.stream().map(DocumentChunk::getText).toList());
        store.upsert(chunks, vectors, embeddings.modelName());
    }

    public List<RetrievedChunk> retrieve(String query, String courseId, int k) {
        if (query == null || query.isBlank()) return List.of();
        float[] q = embeddings.embed(List.of(query)).get(0);
        return store.search(q, k, courseId, embeddings.modelName()).stream()
                .filter(r -> r.score >= minScore)
                .toList();
    }

    public void removeMaterial(String materialId) {
        store.deleteByMaterial(materialId);
    }

    public void removeCourse(String courseId) {
        store.deleteByCourse(courseId);
    }

    public double getMinScore() { return minScore; }
}
