package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.domain.ActivityType;
import ca.yorku.eecs3311.studyagent.domain.StudyPlanRequest;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import ca.yorku.eecs3311.studyagent.domain.TopicPriority;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Long horizon: each topic is introduced once (READ) and then revisited after 1, 3, 7 and 14 days
 * (flashcards, quiz, flashcards, review). Topics are introduced in priority order as capacity allows.
 */
public class SpacedRepetitionStrategy implements StudyPlanningStrategy {
    private static final int[] OFFSETS = {0, 1, 3, 7, 14};
    private static final ActivityType[] ACTIVITIES = {ActivityType.READ, ActivityType.FLASHCARDS, ActivityType.QUIZ,
            ActivityType.FLASHCARDS, ActivityType.REVIEW};

    @Override public String getName() { return "Spaced repetition"; }

    @Override
    public List<StudyTask> buildSchedule(StudyPlanRequest request, List<TopicPriority> priorities) {
        int days = Math.max(1, request.availableDays());
        int slots = ScheduleSlots.slotsPerDay(request);
        int minutes = ScheduleSlots.slotMinutes(request);
        Map<Integer, Integer> used = new HashMap<>();
        List<StudyTask> tasks = new ArrayList<>();
        int introDay = 0;
        for (TopicPriority t : priorities) {
            while (introDay < days && used.getOrDefault(introDay, 0) >= slots) introDay++;
            if (introDay >= days) break;
            for (int i = 0; i < OFFSETS.length; i++) {
                int day = introDay + OFFSETS[i];
                while (day < days && used.getOrDefault(day, 0) >= slots) day++;
                if (day >= days) break;
                used.merge(day, 1, Integer::sum);
                LocalDate date = request.startDate.plusDays(day);
                tasks.add(ScheduleSlots.task(date, minutes, ACTIVITIES[i], t));
            }
        }
        tasks.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        return tasks;
    }
}
