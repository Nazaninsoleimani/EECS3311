package ca.yorku.eecs3311.studyagent.integration.retrieval;

import ca.yorku.eecs3311.studyagent.domain.DocumentChunk;

public class RetrievedChunk {
    public final DocumentChunk chunk;
    public final double score;

    public RetrievedChunk(DocumentChunk chunk, double score) {
        this.chunk = chunk;
        this.score = score;
    }
}
