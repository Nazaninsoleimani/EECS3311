package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class QuizAttempt {
    private final String id;
    private final String quizId;
    private final String studentId;
    private final String courseId;
    private final LocalDateTime submittedAt;
    private final List<QuestionResponse> responses;
    private final List<GradeResult> results;

    public QuizAttempt(String id, String quizId, String studentId, String courseId,
                       List<QuestionResponse> responses, List<GradeResult> results) {
        this.id = id;
        this.quizId = quizId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.submittedAt = LocalDateTime.now();
        this.responses = List.copyOf(responses);
        this.results = List.copyOf(results);
    }

    /** Derived score: mean of per-question scores, in [0,1]. */
    public double computeScore() {
        return results.stream().mapToDouble(r -> r.score).average().orElse(0.0);
    }

    public long correctCount() {
        return results.stream().filter(r -> r.correct).count();
    }

    public Optional<GradeResult> findResult(String questionId) {
        return results.stream().filter(r -> r.questionId.equals(questionId)).findFirst();
    }

    public Optional<QuestionResponse> findResponse(String questionId) {
        return responses.stream().filter(r -> r.questionId.equals(questionId)).findFirst();
    }

    public String getId() { return id; }
    public String getQuizId() { return quizId; }
    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public List<QuestionResponse> getResponses() { return responses; }
    public List<GradeResult> getResults() { return results; }
}
