package ca.yorku.eecs3311.studyagent.application;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.agent.AgentException;
import ca.yorku.eecs3311.studyagent.config.AppConfig;
import ca.yorku.eecs3311.studyagent.config.AppContext;
import ca.yorku.eecs3311.studyagent.domain.*;
import ca.yorku.eecs3311.studyagent.integration.llm.*;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteDatabase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** F12 alternative flow: the LLM fails mid-session and stored questions are reused. */
class AdaptiveFallbackTest {
    @TempDir Path dir;
    private boolean down;
    private final LLMClient scripted = LearningFacadeTest.scripted();
    private final AppContext app = AppContext.create(new AppConfig(Map.of()), SqliteDatabase.inMemory(), new LLMClient() {
        public LLMResponse generate(LLMRequest r) { check(); return scripted.generate(r); }
        public LLMResponse generateStructured(LLMRequest r, OutputSchema s) { check(); return scripted.generateStructured(r, s); }
        public String getModelName() { return "flaky"; }
        private void check() { if (down) throw new LLMException("service unavailable", true); }
    }, (t, b, to) -> true);
    private final LearningFacade f = app.facade();

    @AfterEach
    void close() { app.close(); }

    private Course courseWithMaterial() throws Exception {
        Course c = f.createCourse(app.studentId(), "EECS3311", "Design", null);
        f.uploadMaterial(c.getId(), Files.writeString(dir.resolve("p.txt"),
                "The observer pattern: a subject notifies its observers. ".repeat(20) + "An adapter converts an interface. ".repeat(20)));
        return c;
    }

    @Test
    void storedQuestionsAreReusedWhenTheLlmFailsThenTheSessionEnds() throws Exception {
        Course c = courseWithMaterial();
        Quiz quiz = f.generateQuiz(new QuizRequest(app.studentId(), c.getId(), List.of(), 2, null, null));
        down = true;

        AdaptiveLearningSession s = f.startAdaptiveSession(app.studentId(), c.getId());
        Question first = s.getCurrentQuestion();
        assertTrue(quiz.findQuestion(first.getId()).isPresent(), "question comes from the earlier quiz");

        ActivityOutcome o1 = f.submitActivityResult(s.getId(), new QuestionResponse(first.getId(), first.correctAnswerText().substring(0, 1)));
        assertNotNull(o1.next());
        assertNotEquals(first.getId(), o1.next().getId(), "never repeats a question");
        assertTrue(o1.notice().contains("reused"));

        ActivityOutcome o2 = f.submitActivityResult(s.getId(), new QuestionResponse(o1.next().getId(), o1.next().correctAnswerText().substring(0, 1)));
        assertNull(o2.next());
        assertTrue(o2.notice().startsWith("The session has to end"), o2.notice());
        assertTrue(o2.notice().contains("mixed review"), "both concepts answered correctly once -> mastered");
        assertEquals(2, f.endAdaptiveSession(s.getId()).getCorrect());
    }

    @Test
    void sessionCannotStartWithoutLlmAndWithoutStoredQuestions() throws Exception {
        Course c = courseWithMaterial();
        down = true;
        AgentException e = assertThrows(AgentException.class, () -> f.startAdaptiveSession(app.studentId(), c.getId()));
        assertTrue(e.getMessage().contains("no unused stored question"));
    }

    @Test
    void bankPrefersTheSessionLevelAndPreferredTypes() {
        AdaptiveLearningSession s = new AdaptiveLearningSession("as", "s1", "c1", new IntermediateState());
        Question mcq = new MultipleChoiceQuestion("q1", "p", DifficultyLevel.INTERMEDIATE, "k", "K", List.of("a", "b", "c"), 0);
        Question tf = new TrueFalseQuestion("q2", "p", DifficultyLevel.INTERMEDIATE, "k", "K", true);
        Question hard = new ShortAnswerQuestion("q3", "p", DifficultyLevel.ADVANCED, "k", "K", "x", List.of());
        assertEquals(0, QuestionBank.rank(s, mcq));
        assertEquals(1, QuestionBank.rank(s, tf), "right level, type not preferred at intermediate");
        assertEquals(2, QuestionBank.rank(s, hard));
    }
}
