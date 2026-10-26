package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDateTime;

/** An uploaded file. Only INDEXED materials can be summarised or used for retrieval. */
public class CourseMaterial {
    private final String id;
    private final String courseId;
    private String title;
    private final FileType fileType;
    private final String sourcePath;
    private MaterialStatus status = MaterialStatus.UPLOADED;
    private String failureReason;
    private final LocalDateTime uploadedAt;
    private int chunkCount;
    private Summary summary;

    public CourseMaterial(String id, String courseId, String title, FileType fileType, String sourcePath) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.fileType = fileType;
        this.sourcePath = sourcePath;
        this.uploadedAt = LocalDateTime.now();
    }

    public void markIndexed(int chunkCount) {
        this.status = MaterialStatus.INDEXED;
        this.chunkCount = chunkCount;
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = MaterialStatus.FAILED;
        this.failureReason = reason;
    }

    public boolean isIndexed() { return status == MaterialStatus.INDEXED; }

    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public FileType getFileType() { return fileType; }
    public String getSourcePath() { return sourcePath; }
    public MaterialStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public int getChunkCount() { return chunkCount; }
    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }

    @Override
    public String toString() {
        return title + " [" + status + "]";
    }
}
