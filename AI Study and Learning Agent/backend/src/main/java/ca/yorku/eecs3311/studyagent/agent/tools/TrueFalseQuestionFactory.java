package ca.yorku.eecs3311.studyagent.agent.tools;

import ca.yorku.eecs3311.studyagent.domain.Question;
import ca.yorku.eecs3311.studyagent.domain.QuestionSpec;
import ca.yorku.eecs3311.studyagent.domain.TrueFalseQuestion;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;

public class TrueFalseQuestionFactory extends QuestionFactory {
    @Override
    protected Question createQuestion(QuestionSpec spec, String id) {
        String a = spec.answer == null ? "" : spec.answer.trim().toLowerCase();
        if (!a.equals("true") && !a.equals("false")) throw new ValidationException("True/false answer must be 'true' or 'false'");
        return new TrueFalseQuestion(id, spec.prompt, spec.difficulty, spec.conceptId, spec.conceptName, a.equals("true"));
    }
}
