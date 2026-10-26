package ca.yorku.eecs3311.studyagent.domain;

import java.util.List;

/** True/false and MCQ; moves up after 3 consecutive correct answers. Lowest level, never moves down. */
public class FoundationState implements DifficultyState {
    @Override public DifficultyLevel getLevel() { return DifficultyLevel.FOUNDATION; }

    @Override
    public void onResult(AdaptiveLearningSession session, GradeResult result) {
        if (session.getStreak() >= AdaptiveLearningSession.PROMOTE_AFTER) session.setState(new IntermediateState());
    }

    @Override
    public List<QuestionType> preferredQuestionTypes() {
        return List.of(QuestionType.TRUE_FALSE, QuestionType.MULTIPLE_CHOICE);
    }
}
