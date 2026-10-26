package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class StudyPlanRequest {
    public final String studentId;
    public final String courseId;
    /** May be null: the plan then covers a default two-week horizon. */
    public final LocalDate examDate;
    public final int dailyMinutes;
    public final List<String> focusTopics;
    public final LocalDate startDate;

    public StudyPlanRequest(String studentId, String courseId, LocalDate examDate, int dailyMinutes,
                            List<String> focusTopics, LocalDate startDate) {
        if (dailyMinutes <= 0) throw new ValidationException("Daily study minutes must be positive");
        this.startDate = startDate == null ? LocalDate.now() : startDate;
        if (examDate != null && !examDate.isAfter(this.startDate)) {
            throw new ValidationException("Exam date must be in the future");
        }
        this.studentId = studentId;
        this.courseId = courseId;
        this.examDate = examDate;
        this.dailyMinutes = dailyMinutes;
        this.focusTopics = focusTopics == null ? List.of() : List.copyOf(focusTopics);
    }

    public StudyPlanRequest(String studentId, String courseId, LocalDate examDate, int dailyMinutes, List<String> focusTopics) {
        this(studentId, courseId, examDate, dailyMinutes, focusTopics, null);
    }

    /** Number of study days before the exam (exam day excluded); 14 when no exam date. */
    public int availableDays() {
        return examDate == null ? 14 : (int) ChronoUnit.DAYS.between(startDate, examDate);
    }
}
