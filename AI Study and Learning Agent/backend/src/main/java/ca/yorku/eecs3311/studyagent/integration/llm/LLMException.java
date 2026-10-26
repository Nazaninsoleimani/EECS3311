package ca.yorku.eecs3311.studyagent.integration.llm;

/** Common error type the adapters translate every vendor failure into. */
public class LLMException extends RuntimeException {
    private final boolean retryable;

    public LLMException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public LLMException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = true;
    }

    public boolean isRetryable() { return retryable; }
}
