package ca.yorku.eecs3311.studyagent.integration.llm;

import ca.yorku.eecs3311.studyagent.config.AppConfig;
import java.time.Duration;

/**
 * Chooses the adapter from configuration; switching provider needs no code change. OpenAI is the default;
 * Gemini remains available with LLM_PROVIDER=gemini.
 */
public final class LLMClientFactory {
    private LLMClientFactory() {}

    public static LLMClient create(AppConfig config) {
        String provider = config.get("LLM_PROVIDER", "openai").toLowerCase();
        Duration timeout = Duration.ofSeconds(config.getInt("LLM_TIMEOUT_SECONDS", 60));
        if (provider.equals("gemini")) {
            if (!config.hasKey("GEMINI_API_KEY")) return missing("GEMINI_API_KEY");
            GeminiSdkClient sdk = new GeminiSdkClient(config.get("GEMINI_API_KEY", ""), timeout);
            return new GeminiLLMAdapter(sdk, config.get("GEMINI_MODEL", "gemini-3.8-flash"));
        }
        if (!config.hasKey("OPENAI_API_KEY")) return missing("OPENAI_API_KEY");
        OpenAISdkClient sdk = new OpenAISdkClient(config.get("OPENAI_BASE_URL", "https://api.openai.com/v1"),
                config.get("OPENAI_API_KEY", ""), timeout);
        return new OpenAILLMAdapter(sdk, config.get("OPENAI_MODEL", "gpt-6.1-sol"));
    }

    private static LLMClient missing(String key) {
        return new UnavailableLLMClient("No LLM configured: set " + key + " in .env (see .env.example). "
                + "AI features are using their non-AI fallbacks.");
    }
}
