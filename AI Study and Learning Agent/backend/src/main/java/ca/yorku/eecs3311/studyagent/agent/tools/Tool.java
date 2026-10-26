package ca.yorku.eecs3311.studyagent.agent.tools;

/** A capability the agent can invoke as a step of its plan. */
public interface Tool {
    String getName();

    String getDescription();

    ToolResult execute(ToolInput input);
}
