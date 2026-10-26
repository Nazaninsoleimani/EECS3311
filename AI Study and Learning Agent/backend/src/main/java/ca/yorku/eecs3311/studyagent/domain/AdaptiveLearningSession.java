package ca.yorku.eecs3311.studyagent.domain;

import java.util.ArrayList;
import java.util.List;

/** Context of the State pattern: delegates difficulty transitions to its current DifficultyState. */
public class AdaptiveLearningSession extends LearningSession {
    public static final int PROMOTE_AFTER = 3;
    public static final int DEMOTE_AFTER = 2;

    private DifficultyState state;
    /** Positive: consecutive correct answers; negative: consecutive incorrect answers. */
    private int streak;
    private Question currentQuestion;
    private int answered;
    private int correct;
    private final List<String> levelHistory = new ArrayList<>();
    private final List<String> recentConceptIds = new ArrayList<>();

    public AdaptiveLearningSession(String id, String studentId, String courseId, DifficultyState initial) {
        super(id, studentId, courseId, SessionType.ADAPTIVE);
        this.state = initial;
        levelHistory.add(initial.getLevel().name());
    }

    public void recordOutcome(GradeResult result) {
        answered++;
        if (result.correct) {
            correct++;
            streak = streak > 0 ? streak + 1 : 1;
        } else {
            streak = streak < 0 ? streak - 1 : -1;
        }
        if (result.conceptId != null) {
            recentConceptIds.add(result.conceptId);
            if (recentConceptIds.size() > 3) recentConceptIds.remove(0);
        }
        state.onResult(this, result);
    }

    /** A level change starts a fresh streak. */
    public void setState(DifficultyState s) {
        this.state = s;
        this.streak = 0;
        levelHistory.add(s.getLevel().name());
    }

    public DifficultyState getState() { return state; }
    public DifficultyLevel getLevel() { return state.getLevel(); }
    public int getStreak() { return streak; }
    public Question getCurrentQuestion() { return currentQuestion; }
    public void setCurrentQuestion(Question q) { this.currentQuestion = q; }
    public int getAnswered() { return answered; }
    public int getCorrect() { return correct; }
    public List<String> getLevelHistory() { return levelHistory; }
    public List<String> getRecentConceptIds() { return recentConceptIds; }
}
