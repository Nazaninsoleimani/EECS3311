package ca.yorku.eecs3311.studyagent.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Concepts (nodes) and relations (edges) of a course, optionally coloured by mastery. */
public class ConceptGraph {
    public final List<Concept> nodes = new ArrayList<>();
    public final List<ConceptRelation> edges = new ArrayList<>();
    /** conceptId -> mastery in [0,1]; absent means "not practised yet". */
    public final Map<String, Double> mastery = new HashMap<>();

    public ConceptGraph() {}

    public ConceptGraph(List<Concept> nodes, List<ConceptRelation> edges) {
        this.nodes.addAll(nodes);
        this.edges.addAll(edges);
    }

    public boolean isEmpty() { return nodes.isEmpty(); }

    public Optional<Concept> find(String conceptId) {
        return nodes.stream().filter(c -> c.getId().equals(conceptId)).findFirst();
    }

    public Optional<Concept> findByName(String name) {
        String key = Concept.normalize(name);
        return nodes.stream().filter(c -> c.normalizedName().equals(key)).findFirst();
    }

    /** Concepts directly connected to the given one, in either direction. */
    public List<Concept> neighbours(String conceptId) {
        Set<String> ids = new LinkedHashSet<>();
        for (ConceptRelation r : edges) {
            if (r.getFromId().equals(conceptId)) ids.add(r.getToId());
            if (r.getToId().equals(conceptId)) ids.add(r.getFromId());
        }
        List<Concept> result = new ArrayList<>();
        for (String id : ids) find(id).ifPresent(result::add);
        return result;
    }

    /** Concepts that are prerequisites of the given one. */
    public List<Concept> prerequisitesOf(String conceptId) {
        List<Concept> result = new ArrayList<>();
        for (ConceptRelation r : edges) {
            if (r.getType() == RelationType.PREREQUISITE_OF && r.getToId().equals(conceptId)) {
                find(r.getFromId()).ifPresent(result::add);
            }
        }
        return result;
    }

    /** Number of concepts this concept is a prerequisite for (used in topic priority). */
    public int dependentsCount(String conceptId) {
        return (int) edges.stream()
                .filter(r -> r.getType() == RelationType.PREREQUISITE_OF && r.getFromId().equals(conceptId))
                .count();
    }

    /** Mermaid "graph TD" text, used by the CLI concept-map command. */
    public String toMermaid() {
        StringBuilder sb = new StringBuilder("graph TD\n");
        for (Concept c : nodes) {
            Double m = mastery.get(c.getId());
            String label = c.getName() + (m == null ? "" : String.format(" (%.0f%%)", m * 100));
            sb.append("  ").append(safeId(c.getId())).append("[\"").append(label.replace("\"", "'")).append("\"]\n");
        }
        for (ConceptRelation r : edges) {
            sb.append("  ").append(safeId(r.getFromId())).append(" -->|").append(r.getType().name().toLowerCase())
              .append("| ").append(safeId(r.getToId())).append('\n');
        }
        return sb.toString();
    }

    private static String safeId(String id) {
        return id.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
