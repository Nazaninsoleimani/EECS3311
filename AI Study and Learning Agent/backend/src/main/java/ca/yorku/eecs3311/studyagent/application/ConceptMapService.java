package ca.yorku.eecs3311.studyagent.application;

import ca.yorku.eecs3311.studyagent.domain.Concept;
import ca.yorku.eecs3311.studyagent.domain.ConceptGraph;
import ca.yorku.eecs3311.studyagent.domain.ConceptMastery;
import ca.yorku.eecs3311.studyagent.domain.ConceptRelation;
import ca.yorku.eecs3311.studyagent.persistence.ConceptRepository;
import ca.yorku.eecs3311.studyagent.persistence.ProgressRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** F04 persistence and F15 concept map: the course's concept graph annotated with the student's mastery. */
public class ConceptMapService {
    private final ConceptRepository concepts;
    private final ProgressRepository progress;

    public ConceptMapService(ConceptRepository concepts, ProgressRepository progress) {
        this.concepts = concepts;
        this.progress = progress;
    }

    /**
     * Stores newly extracted concepts (merged by name with existing ones) and their relations,
     * rewriting relation endpoints to the ids the concepts were actually stored under.
     */
    public ConceptGraph save(String courseId, ConceptGraph extracted) {
        List<Concept> stored = concepts.saveAll(courseId, extracted.nodes);
        Map<String, String> idMap = new HashMap<>();
        for (int i = 0; i < extracted.nodes.size(); i++) idMap.put(extracted.nodes.get(i).getId(), stored.get(i).getId());
        List<ConceptRelation> relations = new ArrayList<>();
        for (ConceptRelation r : extracted.edges) {
            String from = idMap.getOrDefault(r.getFromId(), r.getFromId());
            String to = idMap.getOrDefault(r.getToId(), r.getToId());
            if (!from.equals(to)) relations.add(new ConceptRelation(courseId, from, to, r.getType()));
        }
        concepts.saveRelations(courseId, relations);
        return new ConceptGraph(stored, relations);
    }

    public ConceptGraph buildGraph(String studentId, String courseId) {
        ConceptGraph graph = new ConceptGraph(concepts.findByCourse(courseId), concepts.findRelations(courseId));
        for (ConceptMastery m : progress.findMastery(studentId, courseId)) {
            if (m.getAttempts() > 0) graph.mastery.put(m.getConceptId(), m.getMastery());
        }
        return graph;
    }

    /** Indented text view: each concept with mastery, followed by its outgoing relations. */
    public static String renderText(ConceptGraph graph) {
        if (graph.isEmpty()) return "No concepts yet. Run concept extraction on an uploaded material first.";
        StringBuilder sb = new StringBuilder();
        for (Concept c : graph.nodes) {
            Double m = graph.mastery.get(c.getId());
            sb.append("* ").append(c.getName()).append(m == null ? "  (not practised)" : String.format("  (%.0f%% mastery)", m * 100)).append('\n');
            for (ConceptRelation r : graph.edges) {
                if (!r.getFromId().equals(c.getId())) continue;
                graph.find(r.getToId()).ifPresent(to -> sb.append("    --").append(r.getType().name().toLowerCase())
                        .append("--> ").append(to.getName()).append('\n'));
            }
        }
        return sb.toString().trim();
    }
}
