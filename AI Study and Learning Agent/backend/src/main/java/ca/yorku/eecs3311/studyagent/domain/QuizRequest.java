package ca.yorku.eecs3311.studyagent.domain;

import java.util.List;

public class QuizRequest {
    public final String studentId;
    public final String courseId;
    public final List<String> conceptIds;
    public final int count;
    public final List<QuestionType> types;
    public final DifficultyLevel difficulty;

    public QuizRequest(String studentId, String courseId, List<String> conceptIds, int count,
                       List<QuestionType> types, DifficultyLevel difficulty) {
        if (count < 1 || count > 30) throw new ValidationException("Question count must be between 1 and 30");
        this.studentId = studentId;
        this.courseId = courseId;
        this.conceptIds = conceptIds == null ? List.of() : List.copyOf(conceptIds);
        this.count = count;
        this.types = types == null || types.isEmpty() ? List.of(QuestionType.values()) : List.copyOf(types);
        this.difficulty = difficulty == null ? DifficultyLevel.INTERMEDIATE : difficulty;
    }
}
