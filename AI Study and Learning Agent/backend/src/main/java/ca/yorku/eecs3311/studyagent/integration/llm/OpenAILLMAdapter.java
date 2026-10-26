package ca.yorku.eecs3311.studyagent.integration.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Adapter: translates LLMRequest/LLMResponse to and from the OpenAI chat-completions format. */
public class OpenAILLMAdapter implements LLMClient {
    private final OpenAISdkClient sdk;
    private final String model;

    public OpenAILLMAdapter(OpenAISdkClient sdk, String model) {
        this.sdk = sdk;
        this.model = model;
    }

    @Override
    public LLMResponse generate(LLMRequest request) {
        return call(toOpenAI(request, false));
    }

    @Override
    public LLMResponse generateStructured(LLMRequest request, OutputSchema schema) {
        String system = request.systemPrompt + "\n\nRespond with a single JSON object only, matching this schema ("
                + schema.name + "):\n" + schema.jsonSchema;
        return call(toOpenAI(request.withSystemPrompt(system), true));
    }

    @Override
    public String getModelName() { return "openai:" + model; }

    private LLMResponse call(JsonObject params) {
        try {
            return fromOpenAI(sdk.createChatCompletion(params));
        } catch (LLMException e) {
            if (!e.isRetryable()) throw e;
            return fromOpenAI(sdk.createChatCompletion(params));
        }
    }

    JsonObject toOpenAI(LLMRequest request, boolean json) {
        JsonObject params = new JsonObject();
        params.addProperty("model", model);
        JsonArray messages = new JsonArray();
        if (!request.systemPrompt.isBlank()) messages.add(message("system", request.systemPrompt));
        messages.add(message("user", request.userText()));
        params.add("messages", messages);
        params.addProperty("temperature", request.temperature);
        params.addProperty("max_tokens", request.maxTokens);
        if (json) {
            JsonObject format = new JsonObject();
            format.addProperty("type", "json_object");
            params.add("response_format", format);
        }
        return params;
    }

    LLMResponse fromOpenAI(JsonObject resp) {
        JsonArray choices = resp.has("choices") ? resp.getAsJsonArray("choices") : new JsonArray();
        if (choices.isEmpty()) throw new LLMException("Provider returned no choices", false);
        JsonObject choice = choices.get(0).getAsJsonObject();
        JsonObject msg = choice.getAsJsonObject("message");
        String text = msg != null && msg.has("content") && !msg.get("content").isJsonNull() ? msg.get("content").getAsString() : "";
        int tokens = resp.has("usage") && resp.getAsJsonObject("usage").has("total_tokens")
                ? resp.getAsJsonObject("usage").get("total_tokens").getAsInt() : 0;
        String finish = choice.has("finish_reason") && !choice.get("finish_reason").isJsonNull()
                ? choice.get("finish_reason").getAsString() : "unknown";
        return new LLMResponse(text, tokens, finish);
    }

    private static JsonObject message(String role, String content) {
        JsonObject m = new JsonObject();
        m.addProperty("role", role);
        m.addProperty("content", content);
        return m;
    }
}
