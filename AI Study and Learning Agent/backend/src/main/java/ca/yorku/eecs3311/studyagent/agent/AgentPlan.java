package ca.yorku.eecs3311.studyagent.agent;

import java.util.ArrayList;
import java.util.List;

/** Ordered tool steps chosen by the Planner; can be revised after each observation. */
public class AgentPlan {
    private final AgentGoal goal;
    private final List<PlanStep> steps = new ArrayList<>();
    /** Plan-level decisions the agent acts on, e.g. "mode=map-reduce". */
    private final List<String> notes = new ArrayList<>();

    public AgentPlan(AgentGoal goal) {
        this.goal = goal;
    }

    public AgentPlan add(PlanStep step) {
        steps.add(step);
        return this;
    }

    public PlanStep nextStep() {
        return steps.stream().filter(s -> s.status == StepStatus.PENDING).findFirst().orElse(null);
    }

    public boolean isComplete() { return nextStep() == null; }

    /** Marks every remaining step as skipped (used when an observation makes them pointless). */
    public void skipRemaining() {
        steps.stream().filter(s -> s.status == StepStatus.PENDING).forEach(s -> s.status = StepStatus.SKIPPED);
    }

    public void skip(String toolName) {
        steps.stream().filter(s -> s.status == StepStatus.PENDING && s.toolName.equals(toolName))
                .forEach(s -> s.status = StepStatus.SKIPPED);
    }

    public AgentGoal getGoal() { return goal; }
    public List<PlanStep> getSteps() { return steps; }
    public List<String> getNotes() { return notes; }
    public boolean hasNote(String note) { return notes.contains(note); }
    public void note(String note) { notes.add(note); }

    /** Human-readable trace used by "--trace" in the CLI and the GUI's agent log. */
    public String describe() {
        StringBuilder sb = new StringBuilder("Plan for " + goal.type + ":\n");
        for (int i = 0; i < steps.size(); i++) sb.append("  ").append(i + 1).append(". ").append(steps.get(i)).append('\n');
        if (!notes.isEmpty()) sb.append("  notes: ").append(String.join(", ", notes)).append('\n');
        return sb.toString();
    }
}
