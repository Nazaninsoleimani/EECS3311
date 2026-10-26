package ca.yorku.eecs3311.studyagent.integration.retrieval;

import java.util.List;

public interface EmbeddingService {
    List<float[]> embed(List<String> texts);

    /** Stored with each vector so vectors from different models are never compared. */
    String modelName();

    /** Cosine similarity below which a chunk is not considered relevant for this model. */
    double suggestedMinScore();
}
