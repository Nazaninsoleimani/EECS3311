package ca.yorku.eecs3311.studyagent.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConceptGraphTest {

    private final Concept inheritance = new Concept("k1", "c1", "Inheritance", "is-a reuse");
    private final Concept polymorphism = new Concept("k2", "c1", "Polymorphism", "many forms");
    private final Concept interfaces = new Concept("k3", "c1", "Interfaces", "contracts");

    private ConceptGraph graph() {
        return new ConceptGraph(List.of(inheritance, polymorphism, interfaces), List.of(
                new ConceptRelation("c1", "k1", "k2", RelationType.PREREQUISITE_OF),
                new ConceptRelation("c1", "k3", "k2", RelationType.RELATED_TO)));
    }

    @Test
    void neighboursFollowEdgesInBothDirections() {
        assertEquals(List.of(inheritance, interfaces), graph().neighbours("k2"));
        assertEquals(List.of(polymorphism), graph().neighbours("k1"));
    }

    @Test
    void prerequisitesAndDependents() {
        ConceptGraph g = graph();
        assertEquals(List.of(inheritance), g.prerequisitesOf("k2"));
        assertEquals(1, g.dependentsCount("k1"));
        assertEquals(0, g.dependentsCount("k3"));
    }

    @Test
    void findByNameIsNormalised() {
        assertTrue(graph().findByName("  POLYMORPHISM ").isPresent());
        assertEquals("abstract classes", Concept.normalize("Abstract-Classes!"));
    }

    @Test
    void mermaidContainsMasteryAndEdges() {
        ConceptGraph g = graph();
        g.mastery.put("k1", 0.75);
        String text = g.toMermaid();
        assertTrue(text.contains("Inheritance (75%)"));
        assertTrue(text.contains("k1 -->|prerequisite_of| k2"));
    }
}
