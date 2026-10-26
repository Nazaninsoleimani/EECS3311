package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.domain.Question;
import ca.yorku.eecs3311.studyagent.domain.QuestionSpec;
import ca.yorku.eecs3311.studyagent.domain.ShortAnswerQuestion;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import java.util.List;

public class ShortAnswerQuestionFactory extends QuestionFactory {
    @Override
    protected Question createQuestion(QuestionSpec spec, String id) {
        if (spec.answer == null || spec.answer.isBlank()) throw new ValidationException("Short answer needs a model answer");
        List<String> rubric = spec.rubric == null ? List.of() : spec.rubric.stream().filter(r -> r != null && !r.isBlank()).toList();
        return new ShortAnswerQuestion(id, spec.prompt, spec.difficulty, spec.conceptId, spec.conceptName, spec.answer, rubric);
    }
}
