package ca.yorku.eecs3311.studyagent.domain;

/** Directed relation between two concepts, e.g. "inheritance PREREQUISITE_OF polymorphism". */
public class ConceptRelation {
    private final String courseId;
    private final String fromId;
    private final String toId;
    private final RelationType type;

    public ConceptRelation(String courseId, String fromId, String toId, RelationType type) {
        this.courseId = courseId;
        this.fromId = fromId;
        this.toId = toId;
        this.type = type;
    }

    public String key() { return fromId + "|" + type + "|" + toId; }

    public String getCourseId() { return courseId; }
    public String getFromId() { return fromId; }
    public String getToId() { return toId; }
    public RelationType getType() { return type; }
}
