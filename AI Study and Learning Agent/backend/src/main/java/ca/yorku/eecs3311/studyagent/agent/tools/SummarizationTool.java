package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.agent.AgentContext;
import ca.yorku.eecs3311.studyagent.agent.ContextBuilder;
import ca.yorku.eecs3311.studyagent.agent.PromptTask;
import ca.yorku.eecs3311.studyagent.agent.ResponseValidator;
import ca.yorku.eecs3311.studyagent.agent.ValidationResult;
import ca.yorku.eecs3311.studyagent.domain.DocumentChunk;
import ca.yorku.eecs3311.studyagent.domain.Summary;
import ca.yorku.eecs3311.studyagent.domain.SummaryOptions;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMClient;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMException;
import ca.yorku.eecs3311.studyagent.integration.llm.OutputSchema;
import ca.yorku.eecs3311.studyagent.integration.retrieval.RetrievedChunk;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Single pass: one structured call over all chunks. Map-reduce: summarise groups of chunks
 * (map), then combine the section summaries into the final structured summary (reduce).
 */
public class SummarizationTool implements Tool {
    public static final OutputSchema SCHEMA = new OutputSchema("summary",
            "{\"summary\":\"string\",\"keyPoints\":[\"string\"],\"sourceRefs\":[\"string\"]}", List.of("summary", "keyPoints"));
    static final int GROUP_TOKENS = 5000;

    private final LLMClient llm;
    private final ContextBuilder contextBuilder;
    private final ResponseValidator validator;

    public SummarizationTool(LLMClient llm, ContextBuilder contextBuilder, ResponseValidator validator) {
        this.llm = llm;
        this.contextBuilder = contextBuilder;
        this.validator = validator;
    }

    @Override public String getName() { return "summarization"; }
    @Override public String getDescription() { return "Summarise a material in one pass or map-reduce over sections"; }

    @Override
    public ToolResult execute(ToolInput input) {
        AgentContext ctx = input.get("context");
        List<DocumentChunk> chunks = input.get("chunks");
        SummaryOptions options = input.get("options", new SummaryOptions(null));
        String materialId = input.getString("materialId");
        boolean mapReduce = input.get("mapReduce", false);
        ctx.set("title", input.getString("title")).set("words", String.valueOf(options.targetWords()));
        try {
            String sections = mapReduce ? mapSections(ctx, chunks) : labelled(chunks);
            ctx.set("sections", sections);
            for (int attempt = 0; attempt < 2; attempt++) {
                ValidationResult v = validator.validate(
                        llm.generateStructured(contextBuilder.build(PromptTask.SUMMARIZE_REDUCE, ctx, 0.2, 3000), SCHEMA), SCHEMA);
                if (v.valid) {
                    List<String> keyPoints = strings(v.json.get("keyPoints"));
                    List<String> refs = v.json.has("sourceRefs") ? strings(v.json.get("sourceRefs")) : List.of();
                    return ToolResult.ok(new Summary(materialId, options.length, v.json.get("summary").getAsString(), keyPoints, refs),
                            mapReduce ? "map-reduce summary" : "single-pass summary");
                }
                ctx.trace.add("summary output invalid: " + v.errors);
            }
            return ToolResult.fail("The AI returned an invalid summary twice; please retry");
        } catch (LLMException e) {
            return ToolResult.fail("Summary unavailable, retry later (" + e.getMessage() + ")");
        }
    }

    /** Map step: one plain-text summary per group of chunks that fits the token budget. */
    private String mapSections(AgentContext ctx, List<DocumentChunk> chunks) {
        List<String> sections = new ArrayList<>();
        List<DocumentChunk> group = new ArrayList<>();
        int tokens = 0;
        for (DocumentChunk c : chunks) {
            int cost = ContextBuilder.estimateTokens(c.getText());
            if (!group.isEmpty() && tokens + cost > GROUP_TOKENS) {
                sections.add(summarizeGroup(ctx, group, sections.size() + 1));
                group = new ArrayList<>();
                tokens = 0;
            }
            group.add(c);
            tokens += cost;
        }
        if (!group.isEmpty()) sections.add(summarizeGroup(ctx, group, sections.size() + 1));
        ctx.trace.add("map step produced " + sections.size() + " section summaries");
        ctx.retrieved.clear();
        return String.join("\n\n", sections);
    }

    private String summarizeGroup(AgentContext ctx, List<DocumentChunk> group, int n) {
        ctx.retrieved.clear();
        group.forEach(c -> ctx.retrieved.add(new RetrievedChunk(c, 1.0)));
        String text = llm.generate(contextBuilder.build(PromptTask.SUMMARIZE_SECTION, ctx, 0.2, 800)).text.trim();
        return "Section " + n + " (" + range(group) + "):\n" + text;
    }

    private static String labelled(List<DocumentChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (DocumentChunk c : chunks) sb.append("(").append(c.sourceLabel()).append(")\n").append(c.getText()).append("\n\n");
        return sb.toString().trim();
    }

    private static String range(List<DocumentChunk> group) {
        String first = group.get(0).getPageRef();
        String last = group.get(group.size() - 1).getPageRef();
        if (first == null) return "part " + (group.get(0).getIndex() + 1) + "-" + (group.get(group.size() - 1).getIndex() + 1);
        return first.equals(last) ? first : first + " to " + last;
    }

    static List<String> strings(JsonElement e) {
        List<String> out = new ArrayList<>();
        if (e != null && e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) {
                if (x.isJsonPrimitive() && !x.getAsString().isBlank()) out.add(x.getAsString().trim());
            }
        }
        return out;
    }
}
