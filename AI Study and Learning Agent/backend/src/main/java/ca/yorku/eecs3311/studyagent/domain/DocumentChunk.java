package ca.yorku.eecs3311.studyagent.domain;

/** A slice of a material's text; the unit of retrieval and citation. */
public class DocumentChunk {
    private final String id;
    private final String materialId;
    private final String courseId;
    private final String materialTitle;
    private final int index;
    private final String text;
    private final String pageRef;

    public DocumentChunk(String id, String materialId, String courseId, String materialTitle,
                         int index, String text, String pageRef) {
        this.id = id;
        this.materialId = materialId;
        this.courseId = courseId;
        this.materialTitle = materialTitle;
        this.index = index;
        this.text = text;
        this.pageRef = pageRef;
    }

    /** Human-readable source label used in citations, e.g. "week5.pdf p.3". */
    public String sourceLabel() {
        return materialTitle + (pageRef != null && !pageRef.isBlank() ? " " + pageRef : " #" + (index + 1));
    }

    public String getId() { return id; }
    public String getMaterialId() { return materialId; }
    public String getCourseId() { return courseId; }
    public String getMaterialTitle() { return materialTitle; }
    public int getIndex() { return index; }
    public String getText() { return text; }
    public String getPageRef() { return pageRef; }
}
