package ca.yorku.eecs3311.studyagent.domain;

import java.util.List;

/**
 * State pattern for adaptive sessions (F12). Each level decides its own transition rule and which
 * question types to use; the LLM only generates content at the level the state machine chose.
 */
public interface DifficultyState {
    DifficultyLevel getLevel();

    /** Called after the session updated its streak; may switch the session to another state. */
    void onResult(AdaptiveLearningSession session, GradeResult result);

    List<QuestionType> preferredQuestionTypes();

    static DifficultyState initial(DifficultyLevel level) {
        return switch (level == null ? DifficultyLevel.FOUNDATION : level) {
            case FOUNDATION -> new FoundationState();
            case INTERMEDIATE -> new IntermediateState();
            case ADVANCED -> new AdvancedState();
        };
    }
}
