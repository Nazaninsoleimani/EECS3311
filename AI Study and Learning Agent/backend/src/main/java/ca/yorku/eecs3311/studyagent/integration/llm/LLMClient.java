package ca.yorku.eecs3311.studyagent.integration.llm;

/**
 * Target interface of the Adapter pattern. The agent, tools and grading service depend only on
 * this; vendor APIs are wrapped by GeminiLLMAdapter and OpenAILLMAdapter.
 */
public interface LLMClient {
    LLMResponse generate(LLMRequest request);

    /** Asks for JSON output matching the schema. The caller still validates the result. */
    LLMResponse generateStructured(LLMRequest request, OutputSchema schema);

    String getModelName();

    /** False when no provider is configured; callers then use their deterministic fallback. */
    default boolean isAvailable() { return true; }
}
