package ca.yorku.eecs3311.studyagent.persistence.sqlite;

import ca.yorku.eecs3311.studyagent.domain.Quiz;
import ca.yorku.eecs3311.studyagent.domain.QuizAttempt;
import ca.yorku.eecs3311.studyagent.persistence.QuizRepository;
import java.util.List;
import java.util.Optional;

public class SqliteQuizRepository implements QuizRepository {
    private final SqliteDatabase db;

    public SqliteQuizRepository(SqliteDatabase db) { this.db = db; }

    @Override public void save(Quiz quiz) { db.put("quiz", quiz.getId(), quiz.getCourseId(), null, quiz); }
    @Override public Optional<Quiz> findById(String id) { return db.get("quiz", id, Quiz.class); }
    @Override public List<Quiz> findByCourse(String courseId) { return db.byParent("quiz", courseId, Quiz.class); }

    @Override
    public void saveAttempt(QuizAttempt a) { db.put("attempt", a.getId(), a.getCourseId(), a.getStudentId(), a); }

    @Override public Optional<QuizAttempt> findAttempt(String attemptId) { return db.get("attempt", attemptId, QuizAttempt.class); }

    @Override
    public List<QuizAttempt> findAttempts(String studentId, String courseId) {
        return db.byParent("attempt", courseId, QuizAttempt.class).stream()
                .filter(a -> a.getStudentId().equals(studentId)).toList();
    }

    @Override
    public void deleteByCourse(String courseId) {
        db.deleteByParent("quiz", courseId);
        db.deleteByParent("attempt", courseId);
    }
}
