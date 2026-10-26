package ca.yorku.eecs3311.studyagent.domain;

public class QuestionResponse {
    public final String questionId;
    public final String answerText;

    public QuestionResponse(String questionId, String answerText) {
        this.questionId = questionId;
        this.answerText = answerText;
    }
}
