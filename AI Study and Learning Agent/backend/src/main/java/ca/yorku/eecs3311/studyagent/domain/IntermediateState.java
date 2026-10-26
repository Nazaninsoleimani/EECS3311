package ca.yorku.eecs3311.studyagent.domain;

import java.util.List;

/** MCQ and short answer; up after 3 consecutive correct, down after 2 consecutive incorrect. */
public class IntermediateState implements DifficultyState {
    @Override public DifficultyLevel getLevel() { return DifficultyLevel.INTERMEDIATE; }

    @Override
    public void onResult(AdaptiveLearningSession session, GradeResult result) {
        if (session.getStreak() >= AdaptiveLearningSession.PROMOTE_AFTER) session.setState(new AdvancedState());
        else if (session.getStreak() <= -AdaptiveLearningSession.DEMOTE_AFTER) session.setState(new FoundationState());
    }

    @Override
    public List<QuestionType> preferredQuestionTypes() {
        return List.of(QuestionType.MULTIPLE_CHOICE, QuestionType.SHORT_ANSWER);
    }
}
