package ca.yorku.eecs3311.studyagent.presentation.gui;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.domain.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PracticeViewsTest {
    @TempDir Path dir;
    private final GuiTestSupport t = new GuiTestSupport();

    @AfterEach
    void close() { t.close(); }

    @Test
    void questionPanelTurnsInputsIntoAnswers() {
        Question mcq = new MultipleChoiceQuestion("q1", "Pick <b>", DifficultyLevel.FOUNDATION, null, null, List.of("x", "y", "z"), 2);
        QuestionPanel p = new QuestionPanel(1, mcq);
        assertEquals("", p.answer());
        p.answerWith("2");
        assertEquals("C", p.answer());
        p.showResult(mcq.grade(p.response()));
        assertTrue(p.feedbackLabel().getText().contains("Correct."));

        QuestionPanel tf = new QuestionPanel(2, new TrueFalseQuestion("q2", "T?", DifficultyLevel.FOUNDATION, null, null, false));
        tf.answerWith("1");
        assertEquals("false", tf.answer());
        QuestionPanel sa = new QuestionPanel(3, new ShortAnswerQuestion("q3", "Why?", DifficultyLevel.ADVANCED, null, null, "because", List.of()));
        sa.answerWith("  because  ");
        assertEquals("because", sa.answer());
    }

    @Test
    void quizViewShowsQuestionsAndGradedResults() throws Exception {
        Course c = t.courseWithMaterial(dir);
        QuizView view = new QuizView(t.ctx);
        Quiz quiz = t.app.facade().generateQuiz(new QuizRequest(t.app.studentId(), c.getId(), List.of(), 2, null, null));
        view.displayQuiz(quiz);
        assertEquals(2, view.panels().size());
        view.panels().get(0).answerWith("0");
        view.panels().get(1).answerWith("1");
        QuizAttempt a = t.app.facade().submitQuiz(quiz.getId(), view.panels().stream().map(QuestionPanel::response).toList());
        view.displayResult(a);
        assertTrue(view.resultLabel().getText().startsWith("Score 50%"));
        assertTrue(view.panels().get(1).feedbackLabel().getText().contains("Incorrect"));
    }

    @Test
    void plannerShowsActivePlanAndCompletedTasks() throws Exception {
        Course c = t.courseWithMaterial(dir);
        StudyPlannerView view = new StudyPlannerView(t.ctx);
        view.render();
        assertTrue(view.rationaleArea().getText().contains("No active plan yet"));
        StudyPlan plan = t.app.facade().generateStudyPlan(new StudyPlanRequest(t.app.studentId(), c.getId(), c.getExamDate(), 60, List.of()));
        t.app.facade().completeStudyTask(plan.getTasks().get(0).getId());
        view.render();
        assertTrue(view.rationaleArea().getText().startsWith("Exam cram strategy"));
        assertEquals(plan.getTasks().size(), view.table().getRowCount());
        assertEquals(Boolean.TRUE, view.table().getValueAt(0, 0));
    }

    @Test
    void flashcardViewWalksThroughDueCards() throws Exception {
        Course c = t.courseWithMaterial(dir);
        t.app.facade().generateFlashcards(new FlashcardRequest(t.app.studentId(), c.getId(), List.of(), "observer", 1));
        FlashcardView view = new FlashcardView(t.ctx);
        view.render();
        assertTrue(view.dueLabel().getText().contains("1 card(s) due"));
        assertTrue(view.frontLabel().getText().contains("What does a subject do?"));
        view.revealForTest();
        assertTrue(view.backLabel().getText().contains("Notifies observers"));
        String cardId = t.app.facade().dueFlashcards(c.getId()).get(0).getId();
        view.rateCard(cardId, ReviewRating.EASY);
        assertTrue(view.dueLabel().getText().contains("0 card(s) due"));
    }

    @Test
    void learningSessionShowsOutcomeAndNextQuestion() throws Exception {
        Course c = t.courseWithMaterial(dir);
        LearningSessionView view = new LearningSessionView(t.ctx);
        view.render();
        AdaptiveLearningSession s = t.app.facade().startAdaptiveSession(t.app.studentId(), c.getId());
        view.sessionForTest(s);
        assertNotNull(view.currentQuestion());
        view.showOutcome(t.app.facade().submitActivityResult(s.getId(), new QuestionResponse(s.getCurrentQuestion().getId(), "true")));
        assertTrue(view.logArea().getText().contains("Correct: "));
        assertNotNull(view.currentQuestion(), "next question shown");
    }

    @Test
    void windowRegistersPracticeTabs() {
        List<String> titles = new StudyAgentGui(t.ctx).views().stream().map(BaseView::title).toList();
        assertTrue(titles.containsAll(List.of("Study Plan", "Quiz", "Flashcards", "Learning Session")), titles.toString());
    }
}
