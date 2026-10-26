package ca.yorku.eecs3311.studyagent.agent.tools;

public class ToolResult {
    public final boolean success;
    public final Object payload;
    public final String message;

    private ToolResult(boolean success, Object payload, String message) {
        this.success = success;
        this.payload = payload;
        this.message = message;
    }

    public static ToolResult ok(Object payload, String message) { return new ToolResult(true, payload, message); }
    public static ToolResult ok(Object payload) { return new ToolResult(true, payload, ""); }
    public static ToolResult fail(String message) { return new ToolResult(false, null, message); }

    @SuppressWarnings("unchecked")
    public <T> T payload() { return (T) payload; }
}
