package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDateTime;

/** One piece of graded evidence (kept for weak-topic diagnosis and trends). */
public class PerformanceRecord {
    public final String studentId;
    public final String courseId;
    public final String conceptId;
    public final String questionPrompt;
    public final String studentAnswer;
    public final boolean correct;
    public final double score;
    public final DifficultyLevel difficulty;
    public final ActivityType source;
    public final LocalDateTime timestamp;

    public PerformanceRecord(String studentId, String courseId, String conceptId, String questionPrompt,
                             String studentAnswer, boolean correct, double score,
                             DifficultyLevel difficulty, ActivityType source) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.conceptId = conceptId;
        this.questionPrompt = questionPrompt;
        this.studentAnswer = studentAnswer;
        this.correct = correct;
        this.score = score;
        this.difficulty = difficulty;
        this.source = source;
        this.timestamp = LocalDateTime.now();
    }
}
