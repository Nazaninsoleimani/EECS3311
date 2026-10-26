package ca.yorku.eecs3311.studyagent.domain;

import java.util.List;

/** Short answer and harder MCQ; down after 2 consecutive incorrect. Highest level. */
public class AdvancedState implements DifficultyState {
    @Override public DifficultyLevel getLevel() { return DifficultyLevel.ADVANCED; }

    @Override
    public void onResult(AdaptiveLearningSession session, GradeResult result) {
        if (session.getStreak() <= -AdaptiveLearningSession.DEMOTE_AFTER) session.setState(new IntermediateState());
    }

    @Override
    public List<QuestionType> preferredQuestionTypes() {
        return List.of(QuestionType.SHORT_ANSWER, QuestionType.MULTIPLE_CHOICE);
    }
}
