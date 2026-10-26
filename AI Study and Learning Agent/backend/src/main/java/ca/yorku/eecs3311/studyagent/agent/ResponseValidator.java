package ca.yorku.eecs3311.studyagent.agent;

import ca.yorku.eecs3311.studyagent.integration.llm.LLMResponse;
import ca.yorku.eecs3311.studyagent.integration.llm.OutputSchema;
import ca.yorku.eecs3311.studyagent.integration.retrieval.RetrievedChunk;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic checks on every LLM output before it becomes a domain object. */
public class ResponseValidator {
    private static final Pattern CITATION = Pattern.compile("\\[(\\d+)]");

    public ValidationResult validate(LLMResponse response, OutputSchema schema) {
        JsonObject json = extractJson(response.text);
        if (json == null) return ValidationResult.invalid(List.of("Output is not a JSON object"));
        List<String> errors = new ArrayList<>();
        for (String field : schema.requiredFields) {
            JsonElement e = json.get(field);
            if (e == null || e.isJsonNull()) errors.add("Missing field '" + field + "'");
            else if (e.isJsonArray() && e.getAsJsonArray().isEmpty()) errors.add("Field '" + field + "' is empty");
            else if (e.isJsonPrimitive() && e.getAsString().isBlank()) errors.add("Field '" + field + "' is blank");
        }
        return errors.isEmpty() ? ValidationResult.ok(json) : ValidationResult.invalid(errors);
    }

    /** Grounded = cites at least one source and every cited number refers to a provided source. */
    public boolean checkGrounding(String answer, List<RetrievedChunk> sources) {
        Set<Integer> cited = citedSources(answer);
        if (cited.isEmpty()) return false;
        return cited.stream().allMatch(n -> n >= 1 && n <= sources.size());
    }

    public Set<Integer> citedSources(String answer) {
        Set<Integer> cited = new LinkedHashSet<>();
        Matcher m = CITATION.matcher(answer == null ? "" : answer);
        while (m.find()) cited.add(Integer.parseInt(m.group(1)));
        return cited;
    }

    /** Accepts raw JSON, JSON in a ```json fence, or JSON surrounded by prose. */
    public static JsonObject extractJson(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "");
            int end = t.lastIndexOf("```");
            if (end >= 0) t = t.substring(0, end);
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        try {
            JsonElement e = JsonParser.parseString(t.substring(start, end + 1));
            return e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
