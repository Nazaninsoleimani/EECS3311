package ca.yorku.eecs3311.studyagent.presentation.gui;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.domain.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InsightViewsTest {
    @TempDir Path dir;
    private final GuiTestSupport t = new GuiTestSupport();

    @AfterEach
    void close() { t.close(); }

    private QuizAttempt takeQuiz(Course c, String first, String second) {
        Quiz quiz = t.app.facade().generateQuiz(new QuizRequest(t.app.studentId(), c.getId(), List.of(), 2, null, null));
        return t.app.facade().submitQuiz(quiz.getId(), List.of(new QuestionResponse(quiz.getQuestions().get(0).getId(), first),
                new QuestionResponse(quiz.getQuestions().get(1).getId(), second)));
    }

    @Test
    void progressDashboardIsNotifiedAsAnObserver() throws Exception {
        Course c = t.courseWithMaterial(dir);
        ProgressDashboardView view = new ProgressDashboardView(t.ctx);
        view.render();
        assertTrue(view.summaryLabel().getText().contains("No graded activity yet"));
        takeQuiz(c, "true", "B");
        SwingUtilities.invokeAndWait(() -> { });
        assertTrue(view.liveLabel().getText().startsWith("Updated: "), "observer callback ran");
        assertEquals(2, view.table().getRowCount());
        assertTrue(view.summaryLabel().getText().contains("Quiz average <b>50%"));
        assertEquals("Adapter Pattern", view.table().getValueAt(0, 0), "weakest first");
    }

    @Test
    void conceptMapLoadsGraphAndDescribesSelection() throws Exception {
        Course c = t.courseWithMaterial(dir);
        takeQuiz(c, "true", "A");
        ConceptMapView view = new ConceptMapView(t.ctx);
        view.render();
        assertEquals(2, view.graph().nodes.size());
        Concept adapter = view.graph().findByName("Adapter Pattern").orElseThrow();
        view.selectConcept(adapter.getId());
        String text = view.detailsArea().getText();
        assertTrue(text.contains("converts an interface"));
        assertTrue(text.contains("Mastery: 100%"));
        assertTrue(text.contains("Connected to: Observer Pattern"));
        assertEquals(ConceptMapView.MASTERED, ConceptMapView.colorFor(1.0));
        assertEquals(ConceptMapView.WEAK, ConceptMapView.colorFor(0.2));
        assertEquals(ConceptMapView.UNPRACTISED, ConceptMapView.colorFor(null));
    }

    @Test
    void reminderViewAddsListsAndCancels() throws Exception {
        t.courseWithMaterial(dir);
        ReminderView view = new ReminderView(t.ctx);
        view.render();
        assertFalse(view.whenField().getText().isBlank(), "defaults to tomorrow 18:00");
        view.messageField().setText("Review the observer pattern");
        view.createReminder();
        assertEquals(1, view.table().getRowCount());
        assertEquals("Review the observer pattern", view.table().getValueAt(0, 3));
        view.cancelReminder(t.app.facade().listReminders(t.app.studentId()).get(0).getId());
        assertEquals(0, view.table().getRowCount());
        assertThrows(ValidationException.class, () -> ReminderView.parseWhen("tomorrow"));
    }

    @Test
    void historyFiltersByActivityType() throws Exception {
        Course c = t.courseWithMaterial(dir);
        takeQuiz(c, "true", "A");
        HistoryView view = new HistoryView(t.ctx);
        view.render();
        int all = view.table().getRowCount();
        assertTrue(all >= 5, "course, upload, concepts, quiz generated, quiz completed");
        view.typeBox().setSelectedItem(EventType.QUIZ_COMPLETED);
        assertEquals(1, view.table().getRowCount());
        assertTrue(((String) view.table().getValueAt(0, 2)).startsWith("Scored 100%"));
    }

    @Test
    void dashboardSummarisesTheSelectedCourse() throws Exception {
        Course c = t.courseWithMaterial(dir);
        t.app.facade().generateStudyPlan(new StudyPlanRequest(t.app.studentId(), c.getId(), c.getExamDate(), 60, List.of()));
        t.app.facade().scheduleReminder(new Reminder(Ids.newId("r"), t.app.studentId(), c.getId(), "Flashcards",
                LocalDateTime.now().plusHours(2), Recurrence.NONE));
        String text = new DashboardView(t.ctx).buildOverview(LocalDate.now());
        assertTrue(text.contains("exam in 6 day(s)"), text);
        assertTrue(text.contains("Plan 0% complete (Exam cram)"));
        assertTrue(text.contains("Flashcards"));
        assertTrue(text.contains("No graded practice yet."));
    }

    @Test
    void windowHasAllTwelveViews() {
        List<String> titles = new StudyAgentGui(t.ctx).views().stream().map(BaseView::title).toList();
        assertEquals(List.of("Dashboard", "Courses", "Materials", "Tutor", "Study Plan", "Quiz", "Flashcards", "Learning Session",
                "Progress", "Concept Map", "Reminders", "History"), titles);
    }
}
