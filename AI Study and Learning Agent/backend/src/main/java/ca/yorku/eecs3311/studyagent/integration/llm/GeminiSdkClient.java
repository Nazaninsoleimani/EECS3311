package ca.yorku.eecs3311.studyagent.integration.llm;

import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.Map;

/** Adaptee: thin client for the Google Gemini REST API (vendor-shaped requests and responses). */
public class GeminiSdkClient {
    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private final String apiKey;
    private final HttpJson http;

    public GeminiSdkClient(String apiKey, Duration timeout) {
        this.apiKey = apiKey;
        this.http = new HttpJson(timeout);
    }

    public JsonObject generateContent(String model, JsonObject contents) {
        return http.post(BASE + model + ":generateContent", Map.of("x-goog-api-key", apiKey), contents);
    }

    public JsonObject batchEmbedContents(String model, JsonObject requests) {
        return http.post(BASE + model + ":batchEmbedContents", Map.of("x-goog-api-key", apiKey), requests);
    }
}
