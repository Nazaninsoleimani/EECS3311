package ca.yorku.eecs3311.studyagent.integration.llm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Minimal JSON-over-HTTPS helper shared by the vendor clients (the adaptees). */
public class HttpJson {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final Duration timeout;

    public HttpJson(Duration timeout) {
        this.timeout = timeout;
    }

    public JsonObject post(String url, Map<String, String> headers, JsonObject body) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        headers.forEach(b::header);
        try {
            HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                boolean retryable = resp.statusCode() == 429 || resp.statusCode() >= 500;
                throw new LLMException("Provider returned HTTP " + resp.statusCode() + ": " + abbreviate(resp.body()), retryable);
            }
            return JsonParser.parseString(resp.body()).getAsJsonObject();
        } catch (IOException e) {
            throw new LLMException("Network error calling provider: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LLMException("Interrupted while calling provider", e);
        }
    }

    private static String abbreviate(String s) {
        return s == null ? "" : s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
