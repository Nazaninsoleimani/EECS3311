package ca.yorku.eecs3311.studyagent.agent;

import ca.yorku.eecs3311.studyagent.agent.tools.Tool;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    public Tool getTool(String name) {
        Tool t = tools.get(name);
        if (t == null) throw new IllegalArgumentException("No tool registered with name " + name);
        return t;
    }

    public boolean has(String name) { return tools.containsKey(name); }

    public List<String> describeTools() {
        return tools.values().stream().map(t -> t.getName() + ": " + t.getDescription()).toList();
    }
}
