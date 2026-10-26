package ca.yorku.eecs3311.studyagent.persistence;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.domain.*;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SqliteRepositoriesTest {
    private final SqliteDatabase db = SqliteDatabase.inMemory();

    @AfterEach
    void close() { db.close(); }

    @Test
    void courseRoundTripAndLookupByStudent() {
        CourseRepository repo = new SqliteCourseRepository(db);
        repo.save(new Course("c1", "s1", "EECS3311", "Software Design", LocalDate.of(2026, 12, 12)));
        repo.save(new Course("c2", "s2", "EECS2030", "OOP", null));
        Course loaded = repo.findById("c1").orElseThrow();
        assertEquals("Software Design", loaded.getTitle());
        assertEquals(LocalDate.of(2026, 12, 12), loaded.getExamDate());
        assertEquals(1, repo.findByStudent("s1").size());
        repo.delete("c1");
        assertTrue(repo.findById("c1").isEmpty());
    }

    @Test
    void quizWithMixedQuestionTypesRoundTrips() {
        QuizRepository repo = new SqliteQuizRepository(db);
        Quiz quiz = new Quiz("z1", "c1", DifficultyLevel.INTERMEDIATE, List.of(
                new MultipleChoiceQuestion("q1", "Pick", DifficultyLevel.INTERMEDIATE, "k1", "A", List.of("x", "y"), 1),
                new TrueFalseQuestion("q2", "True?", DifficultyLevel.INTERMEDIATE, "k1", "A", true),
                new ShortAnswerQuestion("q3", "Explain", DifficultyLevel.INTERMEDIATE, "k1", "A", "because", List.of("because"))));
        repo.save(quiz);
        Quiz loaded = repo.findById("z1").orElseThrow();
        assertInstanceOf(MultipleChoiceQuestion.class, loaded.getQuestions().get(0));
        assertInstanceOf(TrueFalseQuestion.class, loaded.getQuestions().get(1));
        assertInstanceOf(ShortAnswerQuestion.class, loaded.getQuestions().get(2));
        assertTrue(loaded.getQuestions().get(0).grade(new QuestionResponse("q1", "B")).correct);
    }

    @Test
    void conceptsAreMergedByNormalisedName() {
        ConceptRepository repo = new SqliteConceptRepository(db);
        List<Concept> first = repo.saveAll("c1", List.of(new Concept("k1", "c1", "Inheritance", "")));
        List<Concept> second = repo.saveAll("c1", List.of(new Concept("k9", "c1", "inheritance", "is-a relation")));
        assertEquals("k1", second.get(0).getId());
        assertEquals(1, repo.findByCourse("c1").size());
        assertEquals("is-a relation", repo.findById("k1").orElseThrow().getDefinition());
        assertEquals(first.get(0).getId(), second.get(0).getId());
    }

    @Test
    void masteryAndPerformanceStoredPerStudent() {
        ProgressRepository repo = new SqliteProgressRepository(db);
        ConceptMastery m = new ConceptMastery("s1", "c1", "k1", "Inheritance");
        m.applyResult(new GradeResult("q", "k1", true, 1.0, ""));
        repo.saveMastery(m);
        repo.savePerformance(new PerformanceRecord("s1", "c1", "k1", "p", "a", true, 1.0, DifficultyLevel.FOUNDATION, ActivityType.QUIZ));
        assertEquals(1.0, repo.findMastery("s1", "c1", "k1").orElseThrow().getMastery());
        assertEquals(1, repo.findMastery("s1", "c1").size());
        assertEquals(0, repo.findMastery("s2", "c1").size());
        assertEquals(1, repo.findPerformance("s1", "c1").size());
    }

    @Test
    void dueRemindersAndPlanLookupByTask() {
        ReminderRepository reminders = new SqliteReminderRepository(db);
        LocalDateTime now = LocalDateTime.of(2026, 11, 1, 12, 0);
        reminders.save(new Reminder("r1", "s1", null, "past", now.minusMinutes(5), Recurrence.NONE));
        reminders.save(new Reminder("r2", "s1", null, "future", now.plusHours(1), Recurrence.NONE));
        assertEquals(List.of("r1"), reminders.findDue(now).stream().map(Reminder::getId).toList());

        StudyPlanRepository plans = new SqliteStudyPlanRepository(db);
        StudyTask task = new StudyTask("t1", LocalDate.now(), 30, ActivityType.READ, "k1", "A", "read");
        plans.save(new StudyPlan("p1", "s1", "c1", "Balanced", "why", List.of(task)));
        assertEquals("p1", plans.findByTask("t1").orElseThrow().getId());
        assertTrue(plans.findActive("s1", "c1").isPresent());
    }
}
