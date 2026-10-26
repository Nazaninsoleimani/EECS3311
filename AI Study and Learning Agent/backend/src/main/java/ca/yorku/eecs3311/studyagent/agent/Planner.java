package ca.yorku.eecs3311.studyagent.agent;

import ca.yorku.eecs3311.studyagent.agent.tools.ToolInput;
import ca.yorku.eecs3311.studyagent.agent.tools.ToolResult;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMClient;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMException;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMRequest;
import java.util.List;
import java.util.Map;

/**
 * Decides which tools a goal needs and in which order, and revises the plan after each
 * observation (e.g. skip generation when retrieval found nothing). For follow-up tutor
 * questions it asks the LLM to rewrite the question into a standalone retrieval query.
 */
public class Planner {
    static final int SINGLE_PASS_TOKEN_LIMIT = 6000;

    private final LLMClient llm;
    private final ContextBuilder contextBuilder;

    public Planner(LLMClient llm, ContextBuilder contextBuilder) {
        this.llm = llm;
        this.contextBuilder = contextBuilder;
    }

    public AgentPlan createPlan(AgentGoal goal, AgentContext ctx) {
        AgentPlan plan = new AgentPlan(goal);
        Map<String, Object> p = goal.parameters;
        String courseId = (String) p.get("courseId");
        switch (goal.type) {
            case ANSWER_QUESTION -> {
                String query = retrievalQuery((String) p.get("question"), (String) p.get("history"), ctx);
                plan.add(step("retrieve", "retrieval", ToolInput.of("query", query, "courseId", courseId, "k", 5),
                        "Answers must be grounded in the student's own material", true));
            }
            case SUMMARIZE -> {
                int tokens = (int) p.getOrDefault("totalTokens", 0);
                boolean mapReduce = tokens > SINGLE_PASS_TOKEN_LIMIT;
                plan.note(mapReduce ? "mode=map-reduce" : "mode=single-pass");
                plan.add(step("summarize", "summarization", ToolInput.of("mapReduce", mapReduce),
                        mapReduce ? "Material (~" + tokens + " tokens) exceeds one prompt: summarise sections, then combine"
                                  : "Material fits in one prompt", true));
            }
            case EXTRACT_CONCEPTS -> plan.add(step("extract", "concept_extraction", ToolInput.of(),
                    "Identify concepts and relations batch by batch", true));
            case GENERATE_STUDY_PLAN, REVISE_STUDY_PLAN -> {
                plan.add(step("analyze", "progress_analysis", ToolInput.of("studentId", ctx.learner.studentId, "courseId", courseId),
                        "Rank concepts by weakness, recency and prerequisite weight", true));
                plan.add(step("retrieve", "retrieval", ToolInput.of("query", p.getOrDefault("focusQuery", ""), "courseId", courseId, "k", 6),
                        "Ground priorities and objectives in course material", false));
                plan.add(step("schedule", "study_schedule", ToolInput.of(),
                        "Allocate days and minutes with a strategy suited to the time left", true));
                if (Boolean.TRUE.equals(p.get("reminders"))) {
                    plan.add(step("remind", "reminder", ToolInput.of(), "Remind the student of upcoming tasks", false));
                }
            }
            case GENERATE_FLASHCARDS -> {
                plan.add(step("retrieve", "retrieval", ToolInput.of("query", p.get("topic"), "courseId", courseId, "k", 6),
                        "Cards must come from the course material", true));
                plan.add(step("generate", "flashcard_generation", ToolInput.of(), "Write question/answer pairs", true));
            }
            case GENERATE_QUIZ, NEXT_ADAPTIVE_ACTIVITY -> {
                if (goal.type == GoalType.NEXT_ADAPTIVE_ACTIVITY) {
                    plan.add(step("analyze", "progress_analysis", ToolInput.of("studentId", ctx.learner.studentId, "courseId", courseId),
                            "Pick the weakest concept for the next activity", true));
                }
                plan.add(step("retrieve", "retrieval", ToolInput.of("query", p.getOrDefault("topic", ""), "courseId", courseId, "k", 6),
                        "Questions must be answerable from the material", false));
                plan.add(step("generate", "quiz_generation", ToolInput.of(), "Generate and validate questions", true));
            }
            case EXPLAIN_ANSWER -> plan.add(step("retrieve", "retrieval", ToolInput.of("query", p.get("question"), "courseId", courseId, "k", 3),
                    "Back the explanation with course material", false));
            case DIAGNOSE_WEAK_TOPICS -> plan.add(step("analyze", "progress_analysis",
                    ToolInput.of("studentId", ctx.learner.studentId, "courseId", courseId),
                    "Deterministic ranking first; diagnosis only with enough evidence", true));
        }
        return plan;
    }

    /** Adjusts the remaining plan to what a step observed. */
    public AgentPlan revise(AgentPlan plan, PlanStep step, ToolResult observation) {
        GoalType type = plan.getGoal().type;
        if (step.toolName.equals("retrieval")) {
            List<?> found = observation.success ? observation.payload() : List.of();
            if (found.isEmpty()) {
                plan.note("no-grounding");
                if (type == GoalType.ANSWER_QUESTION || type == GoalType.GENERATE_FLASHCARDS) plan.skipRemaining();
            }
        }
        if (step.toolName.equals("progress_analysis") && type == GoalType.DIAGNOSE_WEAK_TOPICS && observation.success) {
            plan.note("ranked");
        }
        return plan;
    }

    /**
     * Follow-up questions such as "why is that?" retrieve poorly on their own; with chat history
     * the LLM rewrites them into a standalone query. Falls back to history + question.
     */
    String retrievalQuery(String question, String history, AgentContext ctx) {
        if (history == null || history.isBlank() || !looksLikeFollowUp(question)) return question;
        if (llm.isAvailable()) {
            try {
                ctx.set("history", history).set("question", question);
                LLMRequest req = contextBuilder.build(PromptTask.REWRITE_QUERY, ctx, 0.0, 120);
                String rewritten = llm.generate(req).text.trim();
                if (!rewritten.isBlank() && rewritten.length() < 300) {
                    ctx.trace.add("planner rewrote follow-up query -> " + rewritten);
                    return rewritten;
                }
            } catch (LLMException e) {
                ctx.trace.add("query rewrite unavailable: " + e.getMessage());
            }
        }
        String lastQuestion = history.lines().filter(l -> l.startsWith("Student: ")).reduce((a, b) -> b).orElse("");
        return lastQuestion.replace("Student: ", "") + " " + question;
    }

    static boolean looksLikeFollowUp(String q) {
        String s = " " + q.toLowerCase().replaceAll("[^a-z ]", " ") + " ";
        return q.split("\\s+").length <= 6
                || s.matches(".* (it|that|this|they|them|those|these|its) .*")
                || s.startsWith(" why ") && q.split("\\s+").length <= 8;
    }

    private static PlanStep step(String name, String tool, ToolInput in, String why, boolean required) {
        return new PlanStep(name, tool, in, why, required);
    }
}
