package ca.yorku.eecs3311.studyagent.persistence;

import ca.yorku.eecs3311.studyagent.domain.*;
import java.util.List;
import java.util.Optional;

public interface QuizRepository {
    void save(Quiz quiz);
    Optional<Quiz> findById(String id);
    List<Quiz> findByCourse(String courseId);
    void saveAttempt(QuizAttempt attempt);
    Optional<QuizAttempt> findAttempt(String attemptId);
    List<QuizAttempt> findAttempts(String studentId, String courseId);
    void deleteByCourse(String courseId);
}
