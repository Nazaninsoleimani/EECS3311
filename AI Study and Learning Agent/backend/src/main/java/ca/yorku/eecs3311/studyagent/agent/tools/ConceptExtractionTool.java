package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.agent.AgentContext;
import ca.yorku.eecs3311.studyagent.agent.ContextBuilder;
import ca.yorku.eecs3311.studyagent.agent.PromptTask;
import ca.yorku.eecs3311.studyagent.agent.ResponseValidator;
import ca.yorku.eecs3311.studyagent.agent.ValidationResult;
import ca.yorku.eecs3311.studyagent.domain.Concept;
import ca.yorku.eecs3311.studyagent.domain.ConceptGraph;
import ca.yorku.eecs3311.studyagent.domain.ConceptRelation;
import ca.yorku.eecs3311.studyagent.domain.DocumentChunk;
import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.RelationType;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMClient;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMException;
import ca.yorku.eecs3311.studyagent.integration.llm.OutputSchema;
import ca.yorku.eecs3311.studyagent.integration.retrieval.RetrievedChunk;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extracts concepts and typed relations batch by batch. Concepts are merged by normalised name
 * (reusing known course concepts); relations to unknown concepts and self-relations are dropped.
 */
public class ConceptExtractionTool implements Tool {
    public static final OutputSchema SCHEMA = new OutputSchema("concepts",
            "{\"concepts\":[{\"name\":\"string\",\"definition\":\"string\"}],\"relations\":[{\"from\":\"string\",\"to\":\"string\","
                    + "\"type\":\"PREREQUISITE_OF|PART_OF|RELATED_TO\"}]}", List.of("concepts"));
    static final int BATCH_TOKENS = 3500;
    static final int MAX_BATCHES = 8;

    private final LLMClient llm;
    private final ContextBuilder contextBuilder;
    private final ResponseValidator validator;

    public ConceptExtractionTool(LLMClient llm, ContextBuilder contextBuilder, ResponseValidator validator) {
        this.llm = llm;
        this.contextBuilder = contextBuilder;
        this.validator = validator;
    }

    @Override public String getName() { return "concept_extraction"; }
    @Override public String getDescription() { return "Identify key concepts and their relations in a material"; }

    @Override
    public ToolResult execute(ToolInput input) {
        AgentContext ctx = input.get("context");
        List<DocumentChunk> chunks = input.get("chunks");
        String courseId = input.getString("courseId");
        String materialId = input.getString("materialId");
        List<Concept> known = input.get("known", List.of());

        Map<String, Concept> byName = new LinkedHashMap<>();
        for (Concept k : known) byName.put(k.normalizedName(), k);
        Set<String> fromThisMaterial = new LinkedHashSet<>();
        List<String[]> rawRelations = new ArrayList<>();
        int batches = 0;
        int failed = 0;
        for (List<DocumentChunk> batch : batches(chunks)) {
            if (batches++ >= MAX_BATCHES) break;
            ctx.retrieved.clear();
            batch.forEach(c -> ctx.retrieved.add(new RetrievedChunk(c, 1.0)));
            ctx.set("known", byName.isEmpty() ? "(none yet)"
                    : byName.values().stream().map(Concept::getName).collect(Collectors.joining(", ")));
            try {
                ValidationResult v = validator.validate(
                        llm.generateStructured(contextBuilder.build(PromptTask.EXTRACT_CONCEPTS, ctx, 0.2, 2500), SCHEMA), SCHEMA);
                if (!v.valid) {
                    failed++;
                    continue;
                }
                for (JsonElement e : v.json.getAsJsonArray("concepts")) {
                    JsonObject o = e.getAsJsonObject();
                    String name = text(o, "name");
                    if (name.isBlank() || name.length() > 60) continue;
                    String key = Concept.normalize(name);
                    Concept c = byName.computeIfAbsent(key, k -> new Concept(Ids.newId("k"), courseId, name, text(o, "definition")));
                    if (c.getMaterialId() == null) c.setMaterialId(materialId);
                    fromThisMaterial.add(key);
                }
                if (v.json.has("relations") && v.json.get("relations").isJsonArray()) {
                    for (JsonElement e : v.json.getAsJsonArray("relations")) {
                        JsonObject o = e.getAsJsonObject();
                        rawRelations.add(new String[]{text(o, "from"), text(o, "to"), text(o, "type")});
                    }
                }
            } catch (LLMException e) {
                return ToolResult.fail("Concept extraction unavailable: " + e.getMessage());
            }
        }
        ConceptGraph graph = new ConceptGraph();
        fromThisMaterial.forEach(k -> graph.nodes.add(byName.get(k)));
        Set<String> seen = new LinkedHashSet<>();
        int dropped = 0;
        for (String[] r : rawRelations) {
            Concept from = byName.get(Concept.normalize(r[0]));
            Concept to = byName.get(Concept.normalize(r[1]));
            RelationType type = parseType(r[2]);
            if (from == null || to == null || type == null || from == to) {
                dropped++;
                continue;
            }
            ConceptRelation rel = new ConceptRelation(courseId, from.getId(), to.getId(), type);
            if (seen.add(rel.key())) graph.edges.add(rel);
        }
        String msg = graph.nodes.size() + " concept(s), " + graph.edges.size() + " relation(s)"
                + (dropped > 0 ? ", " + dropped + " invalid relation(s) dropped" : "")
                + (failed > 0 ? ", " + failed + " batch(es) returned invalid output" : "");
        return ToolResult.ok(graph, msg);
    }

    private static List<List<DocumentChunk>> batches(List<DocumentChunk> chunks) {
        List<List<DocumentChunk>> out = new ArrayList<>();
        List<DocumentChunk> cur = new ArrayList<>();
        int tokens = 0;
        for (DocumentChunk c : chunks) {
            int cost = ContextBuilder.estimateTokens(c.getText());
            if (!cur.isEmpty() && tokens + cost > BATCH_TOKENS) {
                out.add(cur);
                cur = new ArrayList<>();
                tokens = 0;
            }
            cur.add(c);
            tokens += cost;
        }
        if (!cur.isEmpty()) out.add(cur);
        return out;
    }

    private static RelationType parseType(String s) {
        try {
            return RelationType.valueOf(s.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String text(JsonObject o, String field) {
        return o.has(field) && !o.get(field).isJsonNull() ? o.get(field).getAsString().trim() : "";
    }
}
