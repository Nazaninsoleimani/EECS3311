package ca.yorku.eecs3311.studyagent.persistence;

import ca.yorku.eecs3311.studyagent.domain.*;
import java.util.List;
import java.util.Optional;

/** Concepts are merged by normalised name within a course. */
public interface ConceptRepository {
    /** Saves concepts, merging with existing ones of the same name; returns the stored concepts. */
    List<Concept> saveAll(String courseId, List<Concept> concepts);
    void saveRelations(String courseId, List<ConceptRelation> relations);
    List<Concept> findByCourse(String courseId);
    Optional<Concept> findById(String conceptId);
    List<ConceptRelation> findRelations(String courseId);
    void deleteByCourse(String courseId);
}
