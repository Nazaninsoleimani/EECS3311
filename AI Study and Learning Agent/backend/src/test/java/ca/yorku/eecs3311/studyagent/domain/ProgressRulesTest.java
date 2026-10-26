package ca.yorku.eecs3311.studyagent.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProgressRulesTest {

    private static GradeResult result(double score) {
        return new GradeResult("q", "k1", score >= 0.5, score, "");
    }

    @Test
    void masteryIsExponentialMovingAverage() {
        ConceptMastery m = new ConceptMastery("s1", "c1", "k1", "Inheritance");
        m.applyResult(result(1.0));
        assertEquals(1.0, m.getMastery(), 1e-9);
        m.applyResult(result(0.0));
        assertEquals(0.7, m.getMastery(), 1e-9);
        m.applyResult(result(0.0));
        assertEquals(0.49, m.getMastery(), 1e-9);
        assertTrue(m.isWeak());
    }

    @Test
    void needsReviewResultsDoNotChangeMastery() {
        ConceptMastery m = new ConceptMastery("s1", "c1", "k1", "Inheritance");
        GradeResult r = result(1.0);
        r.needsReview = true;
        m.applyResult(r);
        assertEquals(0, m.getAttempts());
    }

    @Test
    void leitnerPromotesAndResets() {
        LocalDate today = LocalDate.of(2026, 11, 1);
        Flashcard card = new Flashcard("f1", "d1", "k1", "What is a subclass?", "A class that extends another");
        card.recordReview(ReviewRating.GOOD, today);
        assertEquals(2, card.getBox());
        assertEquals(today.plusDays(2), card.getNextReview());
        card.recordReview(ReviewRating.EASY, today);
        assertEquals(3, card.getBox());
        assertEquals(today.plusDays(4), card.getNextReview());
        card.recordReview(ReviewRating.AGAIN, today);
        assertEquals(1, card.getBox());
        assertEquals(today.plusDays(1), card.getNextReview());
    }

    @Test
    void leitnerBoxIsCapped() {
        LocalDate today = LocalDate.of(2026, 11, 1);
        Flashcard card = new Flashcard("f1", "d1", "k1", "front", "back");
        for (int i = 0; i < 10; i++) card.recordReview(ReviewRating.GOOD, today);
        assertEquals(Flashcard.MAX_BOX, card.getBox());
        assertEquals(today.plusDays(16), card.getNextReview());
    }

    @Test
    void recurringReminderMovesForward() {
        LocalDateTime due = LocalDateTime.of(2026, 11, 1, 19, 0);
        Reminder r = new Reminder("r1", "s1", null, "Review", due, Recurrence.DAILY);
        assertTrue(r.isDue(due.plusMinutes(1)));
        r.markDelivered(due.plusMinutes(1));
        assertEquals(due.plusDays(1), r.getDueAt());
        assertEquals(ReminderStatus.PENDING, r.getStatus());

        Reminder once = new Reminder("r2", "s1", null, "Exam", due, Recurrence.NONE);
        once.markDelivered(due);
        assertEquals(ReminderStatus.SENT, once.getStatus());
        assertFalse(once.isDue(due.plusDays(1)));
    }

    @Test
    void historyFilterByCourseAndType() {
        LearningEvent a = new LearningEvent("e1", "s1", "c1", EventType.QUIZ_COMPLETED, LocalDateTime.of(2026, 10, 20, 10, 0), "", null);
        LearningEvent b = new LearningEvent("e2", "s1", "c2", EventType.QUESTION_ASKED, LocalDateTime.of(2026, 10, 21, 10, 0), "", null);
        LearningEvent c = new LearningEvent("e3", "s1", "c1", EventType.QUESTION_ASKED, LocalDateTime.of(2026, 10, 22, 10, 0), "", null);
        LearningHistory h = new LearningHistory(List.of(a, b, c));
        assertEquals(List.of(c, a), h.filter(new HistoryFilter("c1", null, null, null)));
        assertEquals(List.of(c), h.filter(new HistoryFilter("c1", null, null, List.of(EventType.QUESTION_ASKED))));
        assertEquals(List.of(b, a), h.filter(new HistoryFilter(null, null, LocalDate.of(2026, 10, 21), null)));
    }

    @Test
    void planRequestValidatesInputs() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        assertThrows(ValidationException.class, () -> new StudyPlanRequest("s", "c", start.minusDays(1), 60, null, start));
        assertThrows(ValidationException.class, () -> new StudyPlanRequest("s", "c", start.plusDays(5), 0, null, start));
        assertEquals(5, new StudyPlanRequest("s", "c", start.plusDays(5), 60, null, start).availableDays());
    }

    @Test
    void planCompletionRate() {
        StudyTask t1 = new StudyTask("t1", LocalDate.now(), 30, ActivityType.READ, "k1", "A", "");
        StudyTask t2 = new StudyTask("t2", LocalDate.now(), 30, ActivityType.QUIZ, "k1", "A", "");
        StudyPlan plan = new StudyPlan("p1", "s1", "c1", "Balanced", "", List.of(t1, t2));
        t1.complete();
        assertEquals(0.5, plan.completionRate(), 1e-9);
        assertTrue(plan.coversConcept("k1"));
    }
}
