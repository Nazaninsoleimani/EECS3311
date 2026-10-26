package ca.yorku.eecs3311.studyagent.agent;

import ca.yorku.eecs3311.studyagent.agent.tools.ToolResult;
import ca.yorku.eecs3311.studyagent.integration.retrieval.RetrievedChunk;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Working memory for one goal: learner snapshot, retrieved sources and step results. */
public class AgentContext {
    public final LearnerContext learner;
    public final List<RetrievedChunk> retrieved = new ArrayList<>();
    public final Map<String, ToolResult> results = new LinkedHashMap<>();
    public final List<String> trace = new ArrayList<>();
    public final Map<String, String> variables = new LinkedHashMap<>();

    public AgentContext(LearnerContext learner) {
        this.learner = learner;
    }

    public void add(String stepName, ToolResult result) {
        results.put(stepName, result);
        trace.add(stepName + " -> " + (result.success ? "ok" : "failed") + (result.message.isBlank() ? "" : ": " + result.message));
    }

    public AgentContext set(String name, String value) {
        variables.put(name, value);
        return this;
    }
}
