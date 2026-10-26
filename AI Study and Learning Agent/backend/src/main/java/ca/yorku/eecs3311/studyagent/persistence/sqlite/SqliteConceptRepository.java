package ca.yorku.eecs3311.studyagent.persistence.sqlite;

import ca.yorku.eecs3311.studyagent.domain.Concept;
import ca.yorku.eecs3311.studyagent.domain.ConceptRelation;
import ca.yorku.eecs3311.studyagent.persistence.ConceptRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SqliteConceptRepository implements ConceptRepository {
    private final SqliteDatabase db;

    public SqliteConceptRepository(SqliteDatabase db) { this.db = db; }

    @Override
    public List<Concept> saveAll(String courseId, List<Concept> concepts) {
        Map<String, Concept> existing = new HashMap<>();
        for (Concept c : findByCourse(courseId)) existing.put(c.normalizedName(), c);
        List<Concept> stored = new ArrayList<>();
        for (Concept c : concepts) {
            Concept match = existing.get(c.normalizedName());
            if (match == null) {
                db.put("concept", c.getId(), courseId, null, c);
                existing.put(c.normalizedName(), c);
                stored.add(c);
            } else {
                // keep the existing id so mastery and relations stay attached; fill a missing definition
                if (match.getDefinition().isBlank() && !c.getDefinition().isBlank()) {
                    match.setDefinition(c.getDefinition());
                    db.put("concept", match.getId(), courseId, null, match);
                }
                stored.add(match);
            }
        }
        return stored;
    }

    @Override
    public void saveRelations(String courseId, List<ConceptRelation> relations) {
        for (ConceptRelation r : relations) db.put("relation", courseId + "|" + r.key(), courseId, null, r);
    }

    @Override public List<Concept> findByCourse(String courseId) { return db.byParent("concept", courseId, Concept.class); }
    @Override public Optional<Concept> findById(String conceptId) { return db.get("concept", conceptId, Concept.class); }
    @Override public List<ConceptRelation> findRelations(String courseId) { return db.byParent("relation", courseId, ConceptRelation.class); }

    @Override
    public void deleteByCourse(String courseId) {
        db.deleteByParent("concept", courseId);
        db.deleteByParent("relation", courseId);
    }
}
