package ca.yorku.eecs3311.studyagent.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Local configuration. Precedence: environment variables, then a .env file, then
 * application.properties, then defaults. Real keys live only in the git-ignored .env.
 */
public class AppConfig {
    private final Map<String, String> values = new HashMap<>();

    public AppConfig(Map<String, String> values) {
        this.values.putAll(values);
    }

    public static AppConfig load() {
        Map<String, String> merged = new HashMap<>();
        for (Path dir : List.of(Path.of("").toAbsolutePath(), Path.of(System.getProperty("user.home"), ".study-agent"))) {
            readProperties(dir.resolve("application.properties"), merged);
            readEnvFile(dir.resolve(".env"), merged);
        }
        merged.putAll(System.getenv());
        return new AppConfig(merged);
    }

    static void readEnvFile(Path file, Map<String, String> into) {
        if (!Files.isRegularFile(file)) return;
        try {
            for (String line : Files.readAllLines(file)) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#") || !t.contains("=")) continue;
                String key = t.substring(0, t.indexOf('=')).trim();
                String value = t.substring(t.indexOf('=') + 1).trim();
                if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"") || value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                into.putIfAbsent(key, value);
            }
        } catch (IOException ignored) {
            // unreadable .env behaves like a missing one
        }
    }

    private static void readProperties(Path file, Map<String, String> into) {
        if (!Files.isRegularFile(file)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file)) {
            p.load(r);
            for (String name : p.stringPropertyNames()) into.putIfAbsent(name, p.getProperty(name));
        } catch (IOException ignored) {
            // optional file
        }
    }

    public String get(String key, String defaultValue) {
        String v = values.get(key);
        return v == null || v.isBlank() ? defaultValue : v;
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** True if the value is a real key rather than empty or the example placeholder. */
    public boolean hasKey(String key) {
        String v = get(key, "");
        return !v.isBlank() && !v.startsWith("YOUR_");
    }

    public Path databasePath() {
        return Path.of(get("STUDY_AGENT_DB", Path.of(System.getProperty("user.home"), ".study-agent", "study-agent.db").toString()));
    }
}
