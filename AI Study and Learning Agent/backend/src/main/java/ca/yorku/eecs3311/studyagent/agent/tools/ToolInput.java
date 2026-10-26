package ca.yorku.eecs3311.studyagent.agent.tools;

import java.util.HashMap;
import java.util.Map;

public class ToolInput {
    public final Map<String, Object> parameters = new HashMap<>();

    public static ToolInput of(Object... keyValues) {
        ToolInput in = new ToolInput();
        for (int i = 0; i + 1 < keyValues.length; i += 2) in.parameters.put((String) keyValues[i], keyValues[i + 1]);
        return in;
    }

    public ToolInput with(String key, Object value) {
        parameters.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) parameters.get(key);
    }

    public <T> T get(String key, T defaultValue) {
        T v = get(key);
        return v == null ? defaultValue : v;
    }

    public String getString(String key) {
        Object v = parameters.get(key);
        return v == null ? null : v.toString();
    }
}
