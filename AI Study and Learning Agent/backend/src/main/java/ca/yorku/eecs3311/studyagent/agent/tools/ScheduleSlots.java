package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.domain.ActivityType;
import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.StudyPlanRequest;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import ca.yorku.eecs3311.studyagent.domain.TopicPriority;
import java.time.LocalDate;

/** Shared slot arithmetic for the strategies: each day is split into sessions of about 30 minutes. */
final class ScheduleSlots {
    static final int SESSION_MINUTES = 30;

    private ScheduleSlots() {}

    static int slotsPerDay(StudyPlanRequest r) {
        return Math.max(1, (int) Math.round(r.dailyMinutes / (double) SESSION_MINUTES));
    }

    static int slotMinutes(StudyPlanRequest r) {
        return r.dailyMinutes / slotsPerDay(r);
    }

    static StudyTask task(LocalDate date, int minutes, ActivityType activity, TopicPriority t) {
        String objective = t.objective != null && !t.objective.isBlank() ? t.objective : defaultObjective(activity, t.concept.getName());
        return new StudyTask(Ids.newId("t"), date, minutes, activity, t.concept.getId(), t.concept.getName(), objective);
    }

    static String defaultObjective(ActivityType activity, String concept) {
        return switch (activity) {
            case READ -> "Read and take notes on " + concept;
            case FLASHCARDS -> "Review flashcards for " + concept;
            case QUIZ -> "Take a practice quiz on " + concept;
            case ADAPTIVE_SESSION -> "Adaptive practice on " + concept;
            case REVIEW -> "Mixed review including " + concept;
        };
    }
}
