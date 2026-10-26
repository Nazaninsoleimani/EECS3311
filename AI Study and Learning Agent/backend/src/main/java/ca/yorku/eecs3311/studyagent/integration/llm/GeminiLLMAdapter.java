package ca.yorku.eecs3311.studyagent.integration.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Adapter: translates LLMRequest/LLMResponse to and from the Gemini generateContent format. */
public class GeminiLLMAdapter implements LLMClient {
    private final GeminiSdkClient sdk;
    private final String model;

    public GeminiLLMAdapter(GeminiSdkClient sdk, String model) {
        this.sdk = sdk;
        this.model = model;
    }

    @Override
    public LLMResponse generate(LLMRequest request) {
        return call(toGemini(request, false));
    }

    @Override
    public LLMResponse generateStructured(LLMRequest request, OutputSchema schema) {
        String system = request.systemPrompt + "\n\nRespond with a single JSON object only, matching this schema ("
                + schema.name + "):\n" + schema.jsonSchema;
        return call(toGemini(request.withSystemPrompt(system), true));
    }

    @Override
    public String getModelName() { return "gemini:" + model; }

    private LLMResponse call(JsonObject body) {
        try {
            return fromGemini(sdk.generateContent(model, body));
        } catch (LLMException e) {
            if (!e.isRetryable()) throw e;
            pause();
            return fromGemini(sdk.generateContent(model, body));
        }
    }

    JsonObject toGemini(LLMRequest request, boolean json) {
        JsonObject body = new JsonObject();
        if (!request.systemPrompt.isBlank()) {
            body.add("systemInstruction", textContent(null, request.systemPrompt));
        }
        JsonArray contents = new JsonArray();
        contents.add(textContent("user", request.userText()));
        body.add("contents", contents);
        JsonObject config = new JsonObject();
        config.addProperty("temperature", request.temperature);
        config.addProperty("maxOutputTokens", request.maxTokens);
        if (json) config.addProperty("responseMimeType", "application/json");
        if (model.contains("flash")) {
            // 2.5 Flash "thinking" would consume the output budget; plain generation is enough here.
            JsonObject thinking = new JsonObject();
            thinking.addProperty("thinkingBudget", 0);
            config.add("thinkingConfig", thinking);
        }
        body.add("generationConfig", config);
        return body;
    }

    LLMResponse fromGemini(JsonObject resp) {
        JsonArray candidates = resp.has("candidates") ? resp.getAsJsonArray("candidates") : new JsonArray();
        if (candidates.isEmpty()) {
            String reason = resp.has("promptFeedback") ? resp.get("promptFeedback").toString() : "no candidates";
            throw new LLMException("Gemini returned no answer (" + reason + ")", false);
        }
        JsonObject first = candidates.get(0).getAsJsonObject();
        StringBuilder text = new StringBuilder();
        if (first.has("content") && first.getAsJsonObject("content").has("parts")) {
            for (JsonElement part : first.getAsJsonObject("content").getAsJsonArray("parts")) {
                JsonObject p = part.getAsJsonObject();
                if (p.has("text")) text.append(p.get("text").getAsString());
            }
        }
        int tokens = resp.has("usageMetadata") && resp.getAsJsonObject("usageMetadata").has("totalTokenCount")
                ? resp.getAsJsonObject("usageMetadata").get("totalTokenCount").getAsInt() : 0;
        String finish = first.has("finishReason") ? first.get("finishReason").getAsString() : "UNKNOWN";
        return new LLMResponse(text.toString(), tokens, finish);
    }

    private static JsonObject textContent(String role, String text) {
        JsonObject content = new JsonObject();
        if (role != null) content.addProperty("role", role);
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        parts.add(part);
        content.add("parts", parts);
        return content;
    }

    private static void pause() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
