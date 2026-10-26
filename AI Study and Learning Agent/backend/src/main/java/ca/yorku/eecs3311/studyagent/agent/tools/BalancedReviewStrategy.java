package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.domain.ActivityType;
import ca.yorku.eecs3311.studyagent.domain.StudyPlanRequest;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import ca.yorku.eecs3311.studyagent.domain.TopicPriority;
import java.util.ArrayList;
import java.util.List;

/** Medium horizon or no exam date: round-robin through topics, each pass moving read -> flashcards -> quiz. */
public class BalancedReviewStrategy implements StudyPlanningStrategy {
    private static final ActivityType[] PASSES = {ActivityType.READ, ActivityType.FLASHCARDS, ActivityType.QUIZ};

    @Override public String getName() { return "Balanced review"; }

    @Override
    public List<StudyTask> buildSchedule(StudyPlanRequest request, List<TopicPriority> priorities) {
        List<StudyTask> tasks = new ArrayList<>();
        if (priorities.isEmpty()) return tasks;
        int days = Math.max(1, request.availableDays());
        int slots = ScheduleSlots.slotsPerDay(request);
        int minutes = ScheduleSlots.slotMinutes(request);
        int total = days * slots;
        for (int i = 0; i < total; i++) {
            TopicPriority t = priorities.get(i % priorities.size());
            ActivityType activity = PASSES[Math.min(PASSES.length - 1, i / priorities.size()) % PASSES.length];
            tasks.add(ScheduleSlots.task(request.startDate.plusDays(i / slots), minutes, activity, t));
        }
        return tasks;
    }
}
