package ca.yorku.eecs3311.studyagent.integration.llm;

import java.util.List;

public class LLMRequest {
    public final String systemPrompt;
    /** User-side message parts, concatenated in order. */
    public final List<String> messages;
    public final double temperature;
    public final int maxTokens;

    public LLMRequest(String systemPrompt, List<String> messages, double temperature, int maxTokens) {
        this.systemPrompt = systemPrompt == null ? "" : systemPrompt;
        this.messages = List.copyOf(messages);
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public static LLMRequest of(String systemPrompt, String userMessage) {
        return new LLMRequest(systemPrompt, List.of(userMessage), 0.3, 2048);
    }

    public LLMRequest withSystemPrompt(String newSystemPrompt) {
        return new LLMRequest(newSystemPrompt, messages, temperature, maxTokens);
    }

    public String userText() {
        return String.join("\n\n", messages);
    }
}
