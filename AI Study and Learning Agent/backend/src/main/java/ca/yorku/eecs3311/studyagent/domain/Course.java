package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** A course owns its materials, concepts, plans, quizzes and decks (composition). */
public class Course {
    private final String id;
    private final String studentId;
    private String code;
    private String title;
    private LocalDate examDate;

    public Course(String id, String studentId, String code, String title, LocalDate examDate) {
        if (code == null || code.isBlank()) throw new ValidationException("Course code is required");
        if (title == null || title.isBlank()) throw new ValidationException("Course title is required");
        this.id = id;
        this.studentId = studentId;
        this.code = code.trim().toUpperCase();
        this.title = title.trim();
        this.examDate = examDate;
    }

    /** Days from today until the exam, or -1 when no exam date is set. */
    public int daysUntilExam() {
        return daysUntilExam(LocalDate.now());
    }

    public int daysUntilExam(LocalDate today) {
        if (examDate == null) return -1;
        return (int) ChronoUnit.DAYS.between(today, examDate);
    }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code.trim().toUpperCase(); }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title.trim(); }
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    @Override
    public String toString() {
        return code + " - " + title + (examDate != null ? " (exam " + examDate + ")" : "");
    }
}
