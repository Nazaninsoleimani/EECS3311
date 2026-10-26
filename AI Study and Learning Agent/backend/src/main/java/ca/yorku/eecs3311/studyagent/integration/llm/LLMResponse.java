package ca.yorku.eecs3311.studyagent.integration.llm;

public class LLMResponse {
    public final String text;
    public final int tokensUsed;
    public final String finishReason;

    public LLMResponse(String text, int tokensUsed, String finishReason) {
        this.text = text == null ? "" : text;
        this.tokensUsed = tokensUsed;
        this.finishReason = finishReason;
    }
}
