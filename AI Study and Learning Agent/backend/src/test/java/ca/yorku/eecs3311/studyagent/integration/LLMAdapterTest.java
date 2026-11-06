package ca.yorku.eecs3311.studyagent.integration;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.config.AppConfig;
import ca.yorku.eecs3311.studyagent.integration.llm.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class LLMAdapterTest {

    @Test
    void geminiAdapterBuildsVendorRequestAndMapsResponse() {
        AtomicReference<JsonObject> sent = new AtomicReference<>();
        GeminiSdkClient fake = new GeminiSdkClient("k", Duration.ofSeconds(1)) {
            @Override
            public JsonObject generateContent(String model, JsonObject contents) {
                sent.set(contents);
                return JsonParser.parseString("""
                    {"candidates":[{"content":{"parts":[{"text":"{\\"ok\\":true}"}]},"finishReason":"STOP"}],
                     "usageMetadata":{"totalTokenCount":42}}""").getAsJsonObject();
            }
        };
        LLMClient client = new GeminiLLMAdapter(fake, "gemini-2.5-flash");
        LLMResponse r = client.generateStructured(LLMRequest.of("You are a tutor", "hello"),
                new OutputSchema("test", "{\"ok\":\"boolean\"}", java.util.List.of("ok")));
        assertEquals("{\"ok\":true}", r.text);
        assertEquals(42, r.tokensUsed);
        assertEquals("STOP", r.finishReason);
        JsonObject cfg = sent.get().getAsJsonObject("generationConfig");
        assertEquals("application/json", cfg.get("responseMimeType").getAsString());
        assertTrue(sent.get().toString().contains("You are a tutor"));
        assertEquals("gemini:gemini-2.5-flash", client.getModelName());
    }

    @Test
    void openAiAdapterMapsResponseAndRetriesOnce() {
        int[] calls = {0};
        OpenAISdkClient fake = new OpenAISdkClient("https://example.test/v1", "k", Duration.ofSeconds(1)) {
            @Override
            public JsonObject createChatCompletion(JsonObject params) {
                if (calls[0]++ == 0) throw new LLMException("HTTP 429", true);
                assertEquals("gpt-test", params.get("model").getAsString());
                return JsonParser.parseString("""
                    {"choices":[{"message":{"content":"Hi"},"finish_reason":"stop"}],"usage":{"total_tokens":7}}""")
                        .getAsJsonObject();
            }
        };
        LLMResponse r = new OpenAILLMAdapter(fake, "gpt-test").generate(LLMRequest.of("", "hello"));
        assertEquals("Hi", r.text);
        assertEquals(7, r.tokensUsed);
        assertEquals(2, calls[0]);
    }

    @Test
    void geminiRetriesHighDemandWithBackoffButNotPermanentErrors() {
        int[] calls = {0};
        GeminiSdkClient busyTwice = new GeminiSdkClient("k", Duration.ofSeconds(1)) {
            @Override
            public JsonObject generateContent(String model, JsonObject contents) {
                if (calls[0]++ < 2) throw new LLMException("HTTP 503: high demand", true);
                return JsonParser.parseString("""
                    {"candidates":[{"content":{"parts":[{"text":"ok"}]},"finishReason":"STOP"}]}""").getAsJsonObject();
            }
        };
        long[] fast = {1, 1, 1};
        assertEquals("ok", new GeminiLLMAdapter(busyTwice, "m", fast).generate(LLMRequest.of("", "hi")).text);
        assertEquals(3, calls[0]);

        calls[0] = 0;
        GeminiSdkClient alwaysBusy = new GeminiSdkClient("k", Duration.ofSeconds(1)) {
            @Override
            public JsonObject generateContent(String model, JsonObject contents) {
                calls[0]++;
                throw new LLMException("HTTP 503", true);
            }
        };
        assertThrows(LLMException.class, () -> new GeminiLLMAdapter(alwaysBusy, "m", fast).generate(LLMRequest.of("", "hi")));
        assertEquals(4, calls[0], "first call plus three retries");

        calls[0] = 0;
        GeminiSdkClient badKey = new GeminiSdkClient("k", Duration.ofSeconds(1)) {
            @Override
            public JsonObject generateContent(String model, JsonObject contents) {
                calls[0]++;
                throw new LLMException("HTTP 400: API key not valid", false);
            }
        };
        assertThrows(LLMException.class, () -> new GeminiLLMAdapter(badKey, "m", fast).generate(LLMRequest.of("", "hi")));
        assertEquals(1, calls[0], "permanent errors are not retried");
    }

    @Test
    void geminiEmptyCandidatesIsAnError() {
        GeminiSdkClient fake = new GeminiSdkClient("k", Duration.ofSeconds(1)) {
            @Override
            public JsonObject generateContent(String model, JsonObject contents) {
                return new JsonObject();
            }
        };
        assertThrows(LLMException.class, () -> new GeminiLLMAdapter(fake, "m").generate(LLMRequest.of("", "x")));
    }

    @Test
    void factoryFallsBackWhenKeyIsPlaceholder() {
        LLMClient c = LLMClientFactory.create(new AppConfig(Map.of("GEMINI_API_KEY", "YOUR_LOCAL_API_KEY_HERE")));
        assertFalse(c.isAvailable());
        assertThrows(LLMException.class, () -> c.generate(LLMRequest.of("", "x")));
        assertTrue(LLMClientFactory.create(new AppConfig(Map.of("GEMINI_API_KEY", "abc"))).isAvailable());
    }

    @Test
    void usageMeterCountsTokens() {
        LLMClient stub = new LLMClient() {
            public LLMResponse generate(LLMRequest r) { return new LLMResponse("x", 10, "STOP"); }
            public LLMResponse generateStructured(LLMRequest r, OutputSchema s) { return new LLMResponse("{}", 5, "STOP"); }
            public String getModelName() { return "stub"; }
        };
        UsageMeter meter = new UsageMeter(stub);
        meter.generate(LLMRequest.of("", "a"));
        meter.generateStructured(LLMRequest.of("", "b"), new OutputSchema("s", "{}", java.util.List.of()));
        assertEquals(2, meter.getCalls());
        assertEquals(15, meter.getTokens());
    }
}
