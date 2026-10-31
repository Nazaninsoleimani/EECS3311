package ca.yorku.eecs3311.studyagent.application;

import ca.yorku.eecs3311.studyagent.agent.AgentContext;
import ca.yorku.eecs3311.studyagent.agent.ContextBuilder;
import ca.yorku.eecs3311.studyagent.agent.PromptTask;
import ca.yorku.eecs3311.studyagent.agent.ResponseValidator;
import ca.yorku.eecs3311.studyagent.agent.ValidationResult;
import ca.yorku.eecs3311.studyagent.domain.GradeResult;
import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.Question;
import ca.yorku.eecs3311.studyagent.domain.QuestionResponse;
import ca.yorku.eecs3311.studyagent.domain.Quiz;
import ca.yorku.eecs3311.studyagent.domain.QuizAttempt;
import ca.yorku.eecs3311.studyagent.domain.ShortAnswerQuestion;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMClient;
import ca.yorku.eecs3311.studyagent.integration.llm.LLMException;
import ca.yorku.eecs3311.studyagent.integration.llm.OutputSchema;
import java.util.ArrayList;
import java.util.List;

/**
 * Objective questions are graded deterministically by the question itself (polymorphic grade()).
 * Short answers are graded by the LLM against the rubric; without an LLM, or if its output is
 * invalid, the keyword grade is kept and flagged needsReview so it does not change mastery.
 */
public class GradingService {
    static final OutputSchema SCHEMA = new OutputSchema("grade", "{\"score\":0.5,\"feedback\":\"string\"}", List.of("score", "feedback"));

    private final LLMClient llm;
    private final ContextBuilder contextBuilder;
    private final ResponseValidator validator;

    public GradingService(LLMClient llm, ContextBuilder contextBuilder, ResponseValidator validator) {
        this.llm = llm;
        this.contextBuilder = contextBuilder;
        this.validator = validator;
    }

    public QuizAttempt grade(Quiz quiz, String studentId, List<QuestionResponse> responses) {
        List<GradeResult> results = new ArrayList<>();
        for (Question q : quiz.getQuestions()) {
            QuestionResponse r = responses.stream().filter(x -> q.getId().equals(x.questionId)).findFirst()
                    .orElse(new QuestionResponse(q.getId(), ""));
            results.add(gradeResponse(q, r));
        }
        List<QuestionResponse> all = quiz.getQuestions().stream().map(q -> responses.stream()
                .filter(x -> q.getId().equals(x.questionId)).findFirst().orElse(new QuestionResponse(q.getId(), ""))).toList();
        return new QuizAttempt(Ids.newId("at"), quiz.getId(), studentId, quiz.getCourseId(), all, results);
    }

    public GradeResult gradeResponse(Question question, QuestionResponse response) {
        GradeResult keyword = question.grade(response);
        if (question instanceof ShortAnswerQuestion sa && keyword.needsReview && llm.isAvailable()) {
            return gradeWithRubric(sa, response, keyword);
        }
        return keyword;
    }

    private GradeResult gradeWithRubric(ShortAnswerQuestion q, QuestionResponse r, GradeResult fallback) {
        AgentContext ctx = new AgentContext(null)
                .set("question", q.getPrompt()).set("model", q.getModelAnswer())
                .set("rubric", String.join("; ", q.getRubric())).set("answer", r.answerText);
        try {
            ValidationResult v = validator.validate(llm.generateStructured(
                    contextBuilder.build(PromptTask.GRADE_SHORT_ANSWER, ctx, 0.0, 400), SCHEMA), SCHEMA);
            if (!v.valid) return fallback;
            double score = v.json.get("score").getAsDouble();
            if (Double.isNaN(score) || score < 0 || score > 1) return fallback;
            return new GradeResult(q.getId(), q.getConceptId(), score >= 0.6, score,
                    v.json.get("feedback").getAsString().trim() + " Model answer: " + q.getModelAnswer());
        } catch (LLMException | IllegalStateException | UnsupportedOperationException | NumberFormatException e) {
            return fallback;
        }
    }
}
