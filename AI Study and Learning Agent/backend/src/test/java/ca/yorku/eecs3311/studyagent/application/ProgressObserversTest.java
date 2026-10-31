package ca.yorku.eecs3311.studyagent.application;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.domain.*;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProgressObserversTest {
    private final SqliteDatabase db = SqliteDatabase.inMemory();
    private final SqliteReminderRepository reminders = new SqliteReminderRepository(db);
    private final SqliteLearningHistoryRepository history = new SqliteLearningHistoryRepository(db);
    private final List<String> delivered = new ArrayList<>();
    private boolean gatewayUp = true;
    private final ReminderScheduler scheduler = new ReminderScheduler(reminders,
            (title, body, to) -> gatewayUp && delivered.add(body), history);

    private Reminder reminder(String msg, LocalDateTime at, Recurrence rec) {
        return new Reminder(Ids.newId("r"), "s1", "c1", msg, at, rec);
    }

    @Test
    void dueRemindersAreSentOnceAndRecurringOnesMoveForward() {
        LocalDateTime now = LocalDateTime.now();
        Reminder once = reminders.findById(scheduler.schedule(reminder("read ch.3", now.plusSeconds(1), Recurrence.NONE)).getId()).orElseThrow();
        Reminder daily = scheduler.schedule(reminder("flashcards", now.plusSeconds(1), Recurrence.DAILY));
        assertEquals(0, scheduler.dispatchDue(now));
        assertEquals(2, scheduler.dispatchDue(now.plusMinutes(1)));
        assertEquals(0, scheduler.dispatchDue(now.plusMinutes(2)), "nothing sent twice");
        assertEquals(ReminderStatus.SENT, reminders.findById(once.getId()).orElseThrow().getStatus());
        Reminder movedDaily = reminders.findById(daily.getId()).orElseThrow();
        assertEquals(ReminderStatus.PENDING, movedDaily.getStatus());
        assertTrue(movedDaily.getDueAt().isAfter(now.plusDays(1).minusMinutes(1)));
        assertEquals(List.of("read ch.3", "flashcards"), delivered);
        assertTrue(history.find("s1", HistoryFilter.all()).stream().anyMatch(e -> e.getType() == EventType.REMINDER_SENT));
    }

    @Test
    void failedDeliveryStaysPendingAndPastOrCancelledRemindersAreRejected() {
        gatewayUp = false;
        Reminder r = scheduler.schedule(reminder("quiz", LocalDateTime.now(), Recurrence.NONE));
        assertEquals(0, scheduler.dispatchDue(LocalDateTime.now().plusMinutes(1)));
        assertEquals(ReminderStatus.PENDING, reminders.findById(r.getId()).orElseThrow().getStatus());
        scheduler.cancel(r.getId());
        assertThrows(ValidationException.class, () -> scheduler.cancel(r.getId()));
        assertThrows(ValidationException.class, () -> scheduler.schedule(reminder("late", LocalDateTime.now().minusDays(1), Recurrence.NONE)));
    }

    @Test
    void weakConceptGetsOneReviewReminder() {
        ProgressEvent drop = new ProgressEvent("s1", "c1", "k1", "Inheritance", 0.6, 0.4);
        scheduler.onProgressChanged(drop);
        scheduler.onProgressChanged(drop);
        scheduler.onProgressChanged(new ProgressEvent("s1", "c1", "k2", "Polymorphism", 0.9, 0.85));
        List<Reminder> all = scheduler.list("s1");
        assertEquals(1, all.size());
        assertTrue(all.get(0).getMessage().contains("Inheritance"));
        assertEquals(LocalDate.now().plusDays(1), all.get(0).getDueAt().toLocalDate());
    }

    @Test
    void planNeedsRevisionOnlyForDropsNotRevisitedSoon() {
        LocalDate today = LocalDate.now();
        StudyPlan plan = new StudyPlan("p1", "s1", "c1", "Balanced review", "", List.of(
                new StudyTask("t1", today.plusDays(1), 30, ActivityType.QUIZ, "k1", "Inheritance", ""),
                new StudyTask("t2", today.plusDays(9), 30, ActivityType.QUIZ, "k2", "Polymorphism", "")));
        ProgressEvent smallChange = new ProgressEvent("s1", "c1", "k2", "Polymorphism", 0.8, 0.75);
        ProgressEvent k1Drop = new ProgressEvent("s1", "c1", "k1", "Inheritance", 0.8, 0.5);
        ProgressEvent k2Drop = new ProgressEvent("s1", "c1", "k2", "Polymorphism", 0.8, 0.5);
        assertFalse(StudyPlanMonitor.needsRevision(plan, smallChange, today));
        assertFalse(StudyPlanMonitor.needsRevision(plan, k1Drop, today), "already practised tomorrow");
        assertTrue(StudyPlanMonitor.needsRevision(plan, k2Drop, today), "next practice too far away");
        plan.getTasks().get(0).complete();
        assertTrue(StudyPlanMonitor.needsRevision(plan, k1Drop, today), "completed tasks do not count");
    }
}
