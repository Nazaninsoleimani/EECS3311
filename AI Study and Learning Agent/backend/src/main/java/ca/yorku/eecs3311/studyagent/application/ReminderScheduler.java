package ca.yorku.eecs3311.studyagent.application;

import ca.yorku.eecs3311.studyagent.domain.EventType;
import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.LearningEvent;
import ca.yorku.eecs3311.studyagent.domain.ProgressEvent;
import ca.yorku.eecs3311.studyagent.domain.Recurrence;
import ca.yorku.eecs3311.studyagent.domain.Reminder;
import ca.yorku.eecs3311.studyagent.domain.ReminderStatus;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import ca.yorku.eecs3311.studyagent.integration.notification.NotificationGateway;
import ca.yorku.eecs3311.studyagent.persistence.LearningHistoryRepository;
import ca.yorku.eecs3311.studyagent.persistence.ReminderRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * F14: stores reminders and delivers due ones through a NotificationGateway. As a ProgressObserver it
 * also schedules a review reminder for tomorrow when a concept falls below the weak threshold.
 */
public class ReminderScheduler implements ProgressObserver {
    static final LocalTime REVIEW_TIME = LocalTime.of(18, 0);

    private final ReminderRepository reminders;
    private final NotificationGateway gateway;
    private final LearningHistoryRepository history;
    private ScheduledExecutorService timer;

    public ReminderScheduler(ReminderRepository reminders, NotificationGateway gateway, LearningHistoryRepository history) {
        this.reminders = reminders;
        this.gateway = gateway;
        this.history = history;
    }

    public Reminder schedule(Reminder reminder) {
        if (reminder.getRecurrence() == Recurrence.NONE && reminder.getDueAt().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new ValidationException("Reminder time is in the past");
        }
        reminders.save(reminder);
        history.append(new LearningEvent(reminder.getStudentId(), reminder.getCourseId(), EventType.REMINDER_CREATED,
                "Reminder at " + reminder.getDueAt() + ": " + reminder.getMessage(), reminder.getId()));
        return reminder;
    }

    public void cancel(String reminderId) {
        Reminder r = reminders.findById(reminderId).orElseThrow(() -> new ValidationException("No reminder with id " + reminderId));
        if (r.getStatus() != ReminderStatus.PENDING) throw new ValidationException("Reminder is already " + r.getStatus().name().toLowerCase());
        r.cancel();
        reminders.save(r);
    }

    public List<Reminder> list(String studentId) {
        return reminders.findByStudent(studentId);
    }

    /** Sends every due reminder; failed deliveries stay pending and are retried next time. */
    public int dispatchDue(LocalDateTime now) {
        int sent = 0;
        for (Reminder r : reminders.findDue(now)) {
            if (!gateway.send("Study reminder", r.getMessage(), r.getStudentId())) continue;
            r.markDelivered(now);
            reminders.save(r);
            history.append(new LearningEvent(r.getStudentId(), r.getCourseId(), EventType.REMINDER_SENT, r.getMessage(), r.getId()));
            sent++;
        }
        return sent;
    }

    @Override
    public void onProgressChanged(ProgressEvent event) {
        if (!event.fellBelowWeakThreshold()) return;
        String message = String.format("Review %s: your mastery dropped to %.0f%%", event.conceptName, event.newMastery * 100);
        boolean alreadyPending = reminders.findByStudent(event.studentId).stream()
                .anyMatch(r -> r.getStatus() == ReminderStatus.PENDING && r.getMessage().startsWith("Review " + event.conceptName + ":"));
        if (alreadyPending) return;
        schedule(new Reminder(Ids.newId("r"), event.studentId, event.courseId, message,
                LocalDate.now().plusDays(1).atTime(REVIEW_TIME), Recurrence.NONE));
    }

    /** Checks for due reminders in the background while the GUI or an interactive CLI session is open. */
    public synchronized void start(int intervalSeconds) {
        if (timer != null) return;
        timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "reminder-dispatch");
            t.setDaemon(true);
            return t;
        });
        timer.scheduleAtFixedRate(() -> {
            try {
                dispatchDue(LocalDateTime.now());
            } catch (RuntimeException e) {
                System.err.println("Reminder dispatch failed: " + e.getMessage());
            }
        }, 0, Math.max(5, intervalSeconds), TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (timer != null) timer.shutdownNow();
        timer = null;
    }
}
