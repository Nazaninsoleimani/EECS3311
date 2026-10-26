package ca.yorku.eecs3311.studyagent.agent;

import java.util.HashMap;
import java.util.Map;

public class AgentGoal {
    public final GoalType type;
    public final String description;
    public final Map<String, Object> parameters = new HashMap<>();

    public AgentGoal(GoalType type, String description) {
        this.type = type;
        this.description = description;
    }

    public AgentGoal with(String key, Object value) {
        parameters.put(key, value);
        return this;
    }
}
