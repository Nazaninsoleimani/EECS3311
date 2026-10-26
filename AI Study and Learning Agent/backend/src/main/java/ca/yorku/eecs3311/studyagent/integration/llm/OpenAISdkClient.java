package ca.yorku.eecs3311.studyagent.integration.llm;

import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.Map;

/** Adaptee: thin client for any OpenAI-compatible chat-completions API. */
public class OpenAISdkClient {
    private final String baseUrl;
    private final String apiKey;
    private final HttpJson http;

    public OpenAISdkClient(String baseUrl, String apiKey, Duration timeout) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.http = new HttpJson(timeout);
    }

    public JsonObject createChatCompletion(JsonObject params) {
        return http.post(baseUrl + "/chat/completions", Map.of("Authorization", "Bearer " + apiKey), params);
    }

    public JsonObject createEmbeddings(JsonObject params) {
        return http.post(baseUrl + "/embeddings", Map.of("Authorization", "Bearer " + apiKey), params);
    }
}
