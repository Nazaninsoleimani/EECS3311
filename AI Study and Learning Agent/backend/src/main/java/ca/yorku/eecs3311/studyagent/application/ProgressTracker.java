package ca.yorku.eecs3311.studyagent.application;

import ca.yorku.eecs3311.studyagent.domain.ActivityType;
import ca.yorku.eecs3311.studyagent.domain.ConceptMastery;
import ca.yorku.eecs3311.studyagent.domain.DifficultyLevel;
import ca.yorku.eecs3311.studyagent.domain.GradeResult;
import ca.yorku.eecs3311.studyagent.domain.LearningProgress;
import ca.yorku.eecs3311.studyagent.domain.PerformanceRecord;
import ca.yorku.eecs3311.studyagent.domain.ProgressEvent;
import ca.yorku.eecs3311.studyagent.domain.Question;
import ca.yorku.eecs3311.studyagent.domain.QuestionResponse;
import ca.yorku.eecs3311.studyagent.domain.Quiz;
import ca.yorku.eecs3311.studyagent.domain.QuizAttempt;
import ca.yorku.eecs3311.studyagent.persistence.ConceptRepository;
import ca.yorku.eecs3311.studyagent.persistence.ProgressRepository;
import ca.yorku.eecs3311.studyagent.persistence.StudyPlanRepository;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject of the Observer pattern. Every graded activity (quiz, adaptive question, flashcard review)
 * is recorded here; when a concept's mastery changes, the attached observers (progress dashboard,
 * StudyPlanMonitor, ReminderScheduler) are notified without the grading code knowing about them.
 */
public class ProgressTracker {
    private final List<ProgressObserver> observers = new CopyOnWriteArrayList<>();
    private final ProgressRepository progress;
    private final ConceptRepository concepts;
    private final StudyPlanRepository plans;

    public ProgressTracker(ProgressRepository progress, ConceptRepository concepts, StudyPlanRepository plans) {
        this.progress = progress;
        this.concepts = concepts;
        this.plans = plans;
    }

    public void attach(ProgressObserver o) {
        if (!observers.contains(o)) observers.add(o);
    }

    public void detach(ProgressObserver o) {
        observers.remove(o);
    }

    public void recordAttempt(QuizAttempt attempt, Quiz quiz) {
        for (GradeResult r : attempt.getResults()) {
            Question q = quiz.findQuestion(r.questionId).orElse(null);
            String answer = attempt.findResponse(r.questionId).map(x -> x.answerText).orElse("");
            recordResult(attempt.getStudentId(), attempt.getCourseId(), r, q == null ? "" : q.getPrompt(), answer,
                    q == null ? quiz.getDifficulty() : q.getDifficulty(), ActivityType.QUIZ);
        }
    }

    public void recordActivity(String studentId, String courseId, Question q, QuestionResponse response, GradeResult result) {
        recordResult(studentId, courseId, result, q.getPrompt(), response == null ? "" : response.answerText,
                q.getDifficulty(), ActivityType.ADAPTIVE_SESSION);
    }

    /**
     * Stores the performance record and, for results tied to a concept, updates its mastery
     * (exponential moving average) and notifies observers. Results flagged needsReview are recorded
     * but do not change mastery.
     */
    public void recordResult(String studentId, String courseId, GradeResult result, String prompt, String answer,
                             DifficultyLevel difficulty, ActivityType source) {
        progress.savePerformance(new PerformanceRecord(studentId, courseId, result.conceptId, prompt, answer,
                result.correct, result.score, difficulty, source));
        if (result.conceptId == null || result.needsReview) return;
        ConceptMastery m = progress.findMastery(studentId, courseId, result.conceptId).orElseGet(() ->
                new ConceptMastery(studentId, courseId, result.conceptId,
                        concepts.findById(result.conceptId).map(c -> c.getName()).orElse(result.conceptId)));
        double old = m.getMastery();
        m.applyResult(result);
        progress.saveMastery(m);
        notifyObservers(new ProgressEvent(studentId, courseId, result.conceptId, m.getConceptName(), old, m.getMastery()));
    }

    public LearningProgress getProgress(String studentId, String courseId) {
        return new LearningProgress(progress.findMastery(studentId, courseId), progress.findPerformance(studentId, courseId),
                plans.findActive(studentId, courseId).orElse(null));
    }

    /** A failing observer must not undo the grading that triggered it, so its error is only reported. */
    private void notifyObservers(ProgressEvent event) {
        for (ProgressObserver o : observers) {
            try {
                o.onProgressChanged(event);
            } catch (RuntimeException e) {
                System.err.println("Progress observer " + o.getClass().getSimpleName() + " failed: " + e.getMessage());
            }
        }
    }

    public int observerCount() { return observers.size(); }
}
