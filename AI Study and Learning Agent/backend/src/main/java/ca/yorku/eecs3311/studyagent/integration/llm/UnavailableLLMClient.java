package ca.yorku.eecs3311.studyagent.integration.llm;

/** Used when no API key is configured: every call fails fast so features take their fallback path. */
public class UnavailableLLMClient implements LLMClient {
    private final String reason;

    public UnavailableLLMClient(String reason) {
        this.reason = reason;
    }

    @Override public LLMResponse generate(LLMRequest request) { throw new LLMException(reason, false); }
    @Override public LLMResponse generateStructured(LLMRequest request, OutputSchema schema) { throw new LLMException(reason, false); }
    @Override public String getModelName() { return "none"; }
    @Override public boolean isAvailable() { return false; }
}
