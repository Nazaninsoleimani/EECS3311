package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.domain.StudyPlanRequest;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import ca.yorku.eecs3311.studyagent.domain.TopicPriority;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Context of the Strategy pattern: picks a scheduling strategy from the time left and delegates to it. */
public class StudyScheduleTool implements Tool {
    public static final int CRAM_MAX_DAYS = 7;
    public static final int SPACED_MIN_DAYS = 21;

    private StudyPlanningStrategy strategy;

    @Override public String getName() { return "study_schedule"; }
    @Override public String getDescription() { return "Allocate prioritised topics to days and sessions"; }

    @Override
    public ToolResult execute(ToolInput input) {
        StudyPlanRequest request = input.get("request");
        List<TopicPriority> priorities = input.get("priorities");
        strategy = selectStrategy(request);
        List<StudyTask> tasks = strategy.buildSchedule(request, priorities);
        Set<String> scheduled = tasks.stream().map(StudyTask::getConceptId).collect(Collectors.toSet());
        List<String> unscheduled = priorities.stream().filter(p -> !scheduled.contains(p.concept.getId()))
                .map(p -> p.concept.getName()).toList();
        return ToolResult.ok(new Schedule(strategy.getName(), tasks, unscheduled),
                strategy.getName() + ": " + tasks.size() + " tasks");
    }

    public StudyPlanningStrategy selectStrategy(StudyPlanRequest request) {
        if (request.examDate == null) return new BalancedReviewStrategy();
        int days = request.availableDays();
        if (days <= CRAM_MAX_DAYS) return new ExamCramStrategy();
        if (days >= SPACED_MIN_DAYS) return new SpacedRepetitionStrategy();
        return new BalancedReviewStrategy();
    }

    public StudyPlanningStrategy getStrategy() { return strategy; }

    public record Schedule(String strategyName, List<StudyTask> tasks, List<String> unscheduledTopics) {}
}
