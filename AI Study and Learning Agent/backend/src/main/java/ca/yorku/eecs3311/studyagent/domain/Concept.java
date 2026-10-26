package ca.yorku.eecs3311.studyagent.domain;

import java.util.Locale;

/** A key idea of a course; the unit of mastery tracking. */
public class Concept {
    private final String id;
    private final String courseId;
    private final String name;
    private String definition;
    private String materialId;

    public Concept(String id, String courseId, String name, String definition) {
        if (name == null || name.isBlank()) throw new ValidationException("Concept name is required");
        this.id = id;
        this.courseId = courseId;
        this.name = name.trim();
        this.definition = definition == null ? "" : definition.trim();
    }

    /** Lower-case, single-spaced name used to merge duplicates across materials. */
    public static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    public String normalizedName() { return normalize(name); }

    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public String getName() { return name; }
    public String getDefinition() { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }

    @Override
    public String toString() { return name; }
}
