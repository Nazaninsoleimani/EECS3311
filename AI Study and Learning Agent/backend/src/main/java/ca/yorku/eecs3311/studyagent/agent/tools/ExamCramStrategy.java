package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.domain.ActivityType;
import ca.yorku.eecs3311.studyagent.domain.StudyPlanRequest;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import ca.yorku.eecs3311.studyagent.domain.TopicPriority;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Exam within a week: practice-heavy, weakest topics first and repeated. Only the most urgent
 * topics are cycled (two per available session), and the last day is a mixed review.
 */
public class ExamCramStrategy implements StudyPlanningStrategy {
    @Override public String getName() { return "Exam cram"; }

    @Override
    public List<StudyTask> buildSchedule(StudyPlanRequest request, List<TopicPriority> priorities) {
        List<StudyTask> tasks = new ArrayList<>();
        if (priorities.isEmpty()) return tasks;
        int days = Math.max(1, request.availableDays());
        int slots = ScheduleSlots.slotsPerDay(request);
        int minutes = ScheduleSlots.slotMinutes(request);
        int focus = Math.min(priorities.size(), Math.max(1, days * slots / 2));
        ActivityType[] cycle = {ActivityType.QUIZ, ActivityType.ADAPTIVE_SESSION, ActivityType.FLASHCARDS};
        int next = 0;
        for (int d = 0; d < days; d++) {
            LocalDate date = request.startDate.plusDays(d);
            boolean lastDay = d == days - 1 && days > 1;
            for (int s = 0; s < slots; s++) {
                TopicPriority t = priorities.get(next % focus);
                ActivityType activity = lastDay ? ActivityType.REVIEW : cycle[(next / focus) % cycle.length];
                tasks.add(ScheduleSlots.task(date, minutes, activity, t));
                next++;
            }
        }
        return tasks;
    }
}
