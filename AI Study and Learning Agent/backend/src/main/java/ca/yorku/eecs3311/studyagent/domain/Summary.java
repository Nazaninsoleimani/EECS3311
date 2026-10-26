package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDateTime;
import java.util.List;

public class Summary {
    private final String materialId;
    private final SummaryLength length;
    private final String text;
    private final List<String> keyPoints;
    private final List<String> sourceRefs;
    private final LocalDateTime createdAt;

    public Summary(String materialId, SummaryLength length, String text, List<String> keyPoints, List<String> sourceRefs) {
        this.materialId = materialId;
        this.length = length;
        this.text = text;
        this.keyPoints = List.copyOf(keyPoints);
        this.sourceRefs = List.copyOf(sourceRefs);
        this.createdAt = LocalDateTime.now();
    }

    public String getMaterialId() { return materialId; }
    public SummaryLength getLength() { return length; }
    public String getText() { return text; }
    public List<String> getKeyPoints() { return keyPoints; }
    public List<String> getSourceRefs() { return sourceRefs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
