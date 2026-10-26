package ca.yorku.eecs3311.studyagent.agent;

import ca.yorku.eecs3311.studyagent.integration.llm.LLMRequest;
import ca.yorku.eecs3311.studyagent.integration.retrieval.RetrievedChunk;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds LLM requests from prompt templates. Templates have a system part and a user part separated
 * by a line "---USER---" and use {{placeholders}} filled from the AgentContext variables;
 * {{sources}} and {{learner}} are generated here, within a token budget.
 */
public class ContextBuilder {
    private static final Map<String, String> TEMPLATE_CACHE = new ConcurrentHashMap<>();
    private final int tokenBudget;

    public ContextBuilder(int tokenBudget) {
        this.tokenBudget = tokenBudget;
    }

    public LLMRequest build(PromptTask task, AgentContext ctx) {
        return build(task, ctx, 0.3, 2048);
    }

    public LLMRequest build(PromptTask task, AgentContext ctx, double temperature, int maxTokens) {
        String template = load(task.template);
        String[] parts = template.split("\\n---USER---\\n", 2);
        String system = fill(parts[0], ctx);
        String user = parts.length > 1 ? fill(parts[1], ctx) : "";
        return new LLMRequest(system.trim(), List.of(user.trim()), temperature, maxTokens);
    }

    private String fill(String text, AgentContext ctx) {
        String out = text;
        if (out.contains("{{sources}}")) out = out.replace("{{sources}}", formatSources(rankAndTrim(ctx.retrieved)));
        if (out.contains("{{learner}}")) out = out.replace("{{learner}}", ctx.learner == null ? "" : ctx.learner.describe());
        for (Map.Entry<String, String> v : ctx.variables.entrySet()) {
            out = out.replace("{{" + v.getKey() + "}}", v.getValue() == null ? "" : v.getValue());
        }
        return out.replaceAll("\\{\\{[a-zA-Z_]+}}", "");
    }

    /** Highest-scoring chunks first, dropping the rest once the token budget is used up. */
    List<RetrievedChunk> rankAndTrim(List<RetrievedChunk> chunks) {
        List<RetrievedChunk> sorted = new ArrayList<>(chunks);
        sorted.sort(Comparator.comparingDouble((RetrievedChunk c) -> c.score).reversed());
        List<RetrievedChunk> kept = new ArrayList<>();
        int used = 0;
        for (RetrievedChunk c : sorted) {
            int cost = estimateTokens(c.chunk.getText());
            if (!kept.isEmpty() && used + cost > tokenBudget) break;
            kept.add(c);
            used += cost;
        }
        return kept;
    }

    /** Numbered list so the model can cite [1], [2], ... */
    public static String formatSources(List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) return "(no course material available)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk c = chunks.get(i);
            sb.append('[').append(i + 1).append("] (").append(c.chunk.sourceLabel()).append(")\n")
              .append(c.chunk.getText()).append("\n\n");
        }
        return sb.toString().trim();
    }

    public static int estimateTokens(String text) {
        return text == null ? 0 : text.length() / 4 + 1;
    }

    static String load(String name) {
        return TEMPLATE_CACHE.computeIfAbsent(name, n -> {
            try (InputStream in = ContextBuilder.class.getResourceAsStream("/prompts/" + n + ".txt")) {
                if (in == null) throw new IllegalStateException("Missing prompt template " + n);
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
