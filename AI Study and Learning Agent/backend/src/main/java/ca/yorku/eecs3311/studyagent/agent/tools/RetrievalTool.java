package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.integration.retrieval.RetrievalService;
import ca.yorku.eecs3311.studyagent.integration.retrieval.RetrievedChunk;
import java.util.List;

public class RetrievalTool implements Tool {
    private final RetrievalService retrieval;

    public RetrievalTool(RetrievalService retrieval) {
        this.retrieval = retrieval;
    }

    @Override public String getName() { return "retrieval"; }
    @Override public String getDescription() { return "Find the course material chunks most relevant to a query"; }

    @Override
    public ToolResult execute(ToolInput input) {
        String query = input.getString("query");
        if (query == null || query.isBlank()) return ToolResult.ok(List.of(), "empty query");
        List<RetrievedChunk> found = retrieval.retrieve(query, input.getString("courseId"), input.get("k", 5));
        return ToolResult.ok(found, found.size() + " relevant chunk(s)");
    }
}
