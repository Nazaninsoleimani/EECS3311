package ca.yorku.eecs3311.studyagent.domain;

import java.util.List;

public class FlashcardRequest {
    public final String studentId;
    public final String courseId;
    public final List<String> conceptIds;
    /** Free-text topic used for retrieval when no concept IDs are given. */
    public final String topic;
    public final int count;

    public FlashcardRequest(String studentId, String courseId, List<String> conceptIds, String topic, int count) {
        if (count < 1 || count > 50) throw new ValidationException("Flashcard count must be between 1 and 50");
        this.studentId = studentId;
        this.courseId = courseId;
        this.conceptIds = conceptIds == null ? List.of() : List.copyOf(conceptIds);
        this.topic = topic;
        this.count = count;
    }
}
