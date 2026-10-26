package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Quiz {
    private final String id;
    private final String courseId;
    private final DifficultyLevel difficulty;
    private final LocalDateTime createdAt;
    private final List<Question> questions;
    private String notice;

    public Quiz(String id, String courseId, DifficultyLevel difficulty, List<Question> questions) {
        this.id = id;
        this.courseId = courseId;
        this.difficulty = difficulty;
        this.createdAt = LocalDateTime.now();
        this.questions = new ArrayList<>(questions);
    }

    public Optional<Question> findQuestion(String questionId) {
        return questions.stream().filter(q -> q.getId().equals(questionId)).findFirst();
    }

    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public DifficultyLevel getDifficulty() { return difficulty; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Question> getQuestions() { return questions; }
    /** Shown when fewer questions than requested survived validation. */
    public String getNotice() { return notice; }
    public void setNotice(String notice) { this.notice = notice; }
}
