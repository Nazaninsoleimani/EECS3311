package ca.yorku.eecs3311.studyagent.presentation.gui;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.domain.Reminder;
import ca.yorku.eecs3311.studyagent.domain.ReminderStatus;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** F14 additions: early reminders before the reminder time, exam reminders, and in-app delivery. */
class ReminderFeaturesTest {
    @TempDir Path dir;
    private final GuiTestSupport t = new GuiTestSupport();

    @AfterEach
    void close() {
        ReminderNotifier.signedIn(null, null);
        t.close();
    }

    private List<Reminder> pending() {
        return t.app.facade().listReminders(t.app.studentId()).stream()
                .filter(r -> r.getStatus() == ReminderStatus.PENDING)
                .sorted(Comparator.comparing(Reminder::getDueAt)).toList();
    }

    @Test
    void earlyReminderIsAddedBeforeTheReminderTime() throws Exception {
        t.courseWithMaterial(dir);
        ReminderView view = new ReminderView(t.ctx);
        view.render();
        LocalDateTime at = LocalDateTime.now().plusHours(2).withSecond(0).withNano(0);
        view.whenField().setText(at.format(ReminderView.WHEN));
        view.messageField().setText("Review the observer pattern");
        view.beforeBox().setSelectedIndex(2);   // 15 minutes before
        view.createReminder();

        List<Reminder> rs = pending();
        assertEquals(2, rs.size());
        assertEquals(at.minusMinutes(15), rs.get(0).getDueAt());
        assertEquals("In 15 min: Review the observer pattern", rs.get(0).getMessage());
        assertEquals(at, rs.get(1).getDueAt());
        assertEquals(2, view.table().getRowCount());
    }

    @Test
    void earlyReminderInThePastIsSkipped() throws Exception {
        t.courseWithMaterial(dir);
        ReminderView view = new ReminderView(t.ctx);
        view.render();
        view.whenField().setText(LocalDateTime.now().plusMinutes(10).format(ReminderView.WHEN));
        view.messageField().setText("Soon");
        view.beforeBox().setSelectedIndex(4);   // 1 hour before: already past
        view.createReminder();
        assertEquals(1, pending().size());
    }

    @Test
    void examRemindersAreAddedOnceAndOnlyInTheFuture() throws Exception {
        t.courseWithMaterial(dir);   // exam in 6 days: the 7-days-before reminder is already past
        ReminderView view = new ReminderView(t.ctx);
        view.render();
        view.createExamReminders();
        List<Reminder> rs = pending();
        assertEquals(2, rs.size());
        assertTrue(rs.get(0).getMessage().startsWith("EECS3311 exam in 3 days"));
        assertTrue(rs.get(1).getMessage().startsWith("EECS3311 exam tomorrow"));
        view.createExamReminders();
        assertEquals(2, pending().size(), "no duplicates");
    }

    @Test
    void notifierKeepsRemindersOfOtherStudentsPending() {
        ReminderNotifier n = new ReminderNotifier();
        assertFalse(n.send("Study reminder", "x", "someone"), "nobody signed in");
        ReminderNotifier.signedIn("other-student", null);
        assertFalse(n.send("Study reminder", "x", "someone"), "a different student is signed in");
    }

    @Test
    void leadLabels() {
        assertEquals("5 min", ReminderView.leadLabel(5));
        assertEquals("1 hour", ReminderView.leadLabel(60));
        assertEquals("1 day", ReminderView.leadLabel(24 * 60));
    }
}
