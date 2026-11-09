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
        if (isReasoningModel(model)) {
            // Newer OpenAI models (o-series, gpt-5 and later) reject max_tokens and custom temperatures, and count
            // hidden reasoning against the output budget; low effort keeps answers fast, the headroom keeps them whole.
            params.addProperty("max_completion_tokens", request.maxTokens + 1024);
            params.addProperty("reasoning_effort", "low");
        } else {
            params.addProperty("temperature", request.temperature);
            params.addProperty("max_tokens", request.maxTokens);
        }
        if (json) {
            JsonObject format = new JsonObject();
            format.addProperty("type", "json_object");
            params.add("response_format", format);
        }
        return params;
    }

    /** o1/o3/o4... and gpt-5 or later; classic chat models (gpt-3.5, gpt-4, gpt-4o, gpt-4.1) and non-OpenAI models are not. */
    static boolean isReasoningModel(String model) {
        String m = model.toLowerCase();
        if (m.matches("^o\\d.*")) return true;
        java.util.regex.Matcher g = java.util.regex.Pattern.compile("^gpt-(\\d+)").matcher(m);
        return g.find() && Integer.parseInt(g.group(1)) >= 5;
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
