package ca.yorku.eecs3311.studyagent.agent;

import ca.yorku.eecs3311.studyagent.agent.tools.ToolInput;

public class PlanStep {
    public final String name;
    public final String toolName;
    public final ToolInput input;
    public final String rationale;
    public StepStatus status = StepStatus.PENDING;
    /** If true, a failure of this step aborts the plan; otherwise the agent continues. */
    public final boolean required;

    public PlanStep(String name, String toolName, ToolInput input, String rationale, boolean required) {
        this.name = name;
        this.toolName = toolName;
        this.input = input;
        this.rationale = rationale;
        this.required = required;
    }

    @Override
    public String toString() {
        return name + " (" + toolName + ", " + status + "): " + rationale;
    }
}
