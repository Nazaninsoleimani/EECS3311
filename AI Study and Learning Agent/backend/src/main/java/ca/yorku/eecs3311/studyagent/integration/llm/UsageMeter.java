package ca.yorku.eecs3311.studyagent.integration.llm;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Decorates any LLMClient to count calls and tokens (shown by "study status"). */
public class UsageMeter implements LLMClient {
    private final LLMClient inner;
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicLong tokens = new AtomicLong();

    public UsageMeter(LLMClient inner) {
        this.inner = inner;
    }

    @Override
    public LLMResponse generate(LLMRequest request) {
        return count(inner.generate(request));
    }

    @Override
    public LLMResponse generateStructured(LLMRequest request, OutputSchema schema) {
        return count(inner.generateStructured(request, schema));
    }

    private LLMResponse count(LLMResponse r) {
        calls.incrementAndGet();
        tokens.addAndGet(r.tokensUsed);
        return r;
    }

    @Override public String getModelName() { return inner.getModelName(); }
    @Override public boolean isAvailable() { return inner.isAvailable(); }

    public int getCalls() { return calls.get(); }
    public long getTokens() { return tokens.get(); }
}
