package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.domain.DifficultyLevel;
import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.Question;
import ca.yorku.eecs3311.studyagent.domain.QuestionSpec;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;

/**
 * Creator of the Factory Method pattern. build() runs the checks common to every question type,
 * then the subclass's createQuestion() validates type-specific constraints and creates the product.
 */
public abstract class QuestionFactory {
    static final int MAX_PROMPT_LENGTH = 600;

    public final Question build(QuestionSpec spec) {
        if (spec == null) throw new ValidationException("Missing question spec");
        if (spec.prompt == null || spec.prompt.isBlank()) throw new ValidationException("Question prompt is empty");
        if (spec.prompt.length() > MAX_PROMPT_LENGTH) throw new ValidationException("Question prompt is too long");
        if (spec.difficulty == null) spec.difficulty = DifficultyLevel.INTERMEDIATE;
        return createQuestion(spec, Ids.newId("q"));
    }

    protected abstract Question createQuestion(QuestionSpec spec, String id);
}
