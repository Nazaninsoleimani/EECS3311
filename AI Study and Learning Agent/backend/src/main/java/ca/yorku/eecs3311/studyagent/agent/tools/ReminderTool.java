package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.Recurrence;
import ca.yorku.eecs3311.studyagent.domain.Reminder;
import ca.yorku.eecs3311.studyagent.domain.StudyPlan;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Lets the agent request reminders for its study plan: one per planned day (first 7 days) at the
 * configured time. Scheduling itself stays in ReminderScheduler, passed in as a callback so the
 * agent layer does not depend on the application layer.
 */
public class ReminderTool implements Tool {
    private final Consumer<Reminder> scheduler;
    private final LocalTime reminderTime;

    public ReminderTool(Consumer<Reminder> scheduler, LocalTime reminderTime) {
        this.scheduler = scheduler;
        this.reminderTime = reminderTime;
    }

    @Override public String getName() { return "reminder"; }
    @Override public String getDescription() { return "Schedule study reminders for planned tasks"; }

    @Override
    public ToolResult execute(ToolInput input) {
        StudyPlan plan = input.get("plan");
        LocalDateTime now = LocalDateTime.now();
        List<Reminder> created = new ArrayList<>();
        plan.getTasks().stream().collect(Collectors.groupingBy(StudyTask::getDate)).entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .limit(7)
                .forEach(e -> {
                    LocalDate day = e.getKey();
                    LocalDateTime at = day.atTime(reminderTime);
                    if (!at.isAfter(now)) return;
                    String topics = e.getValue().stream().map(StudyTask::getConceptName).distinct().collect(Collectors.joining(", "));
                    Reminder r = new Reminder(Ids.newId("r"), plan.getStudentId(), plan.getCourseId(),
                            "Study plan today: " + topics, at, Recurrence.NONE);
                    scheduler.accept(r);
                    created.add(r);
                });
        return ToolResult.ok(created, created.size() + " reminder(s) scheduled");
    }
}
