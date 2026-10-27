package ca.yorku.eecs3311.studyagent.agent;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.agent.tools.*;
import ca.yorku.eecs3311.studyagent.domain.*;
import ca.yorku.eecs3311.studyagent.integration.llm.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class GenerationToolsTest {

    /** Deterministic fake LLM: answers structured calls with a function of the request. */
    static LLMClient fake(Function<LLMRequest, String> structured, Function<LLMRequest, String> plain, List<LLMRequest> log) {
        return new LLMClient() {
            public LLMResponse generate(LLMRequest r) { log.add(r); return new LLMResponse(plain.apply(r), 5, "STOP"); }
            public LLMResponse generateStructured(LLMRequest r, OutputSchema s) { log.add(r); return new LLMResponse(structured.apply(r), 5, "STOP"); }
            public String getModelName() { return "fake"; }
        };
    }

    private static AgentContext ctx() {
        return new AgentContext(new LearnerContext("s", "c1", new StudentProfile(), List.of(), List.of(), null));
    }

    private static List<DocumentChunk> chunks(int n, int words) {
        List<DocumentChunk> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(new DocumentChunk("ch" + i, "m1", "c1", "w5.pdf", i, "word ".repeat(words), "p." + (i + 1)));
        return out;
    }

    @Test
    void summarizationMapReduceCallsOncePerGroupThenReduces() {
        List<LLMRequest> log = new ArrayList<>();
        LLMClient llm = fake(r -> "{\"summary\":\"S\",\"keyPoints\":[\"k1\",\"k2\"],\"sourceRefs\":[\"p.1\"]}", r -> "section text", log);
        SummarizationTool tool = new SummarizationTool(llm, new ContextBuilder(100000), new ResponseValidator());
        ToolResult r = tool.execute(ToolInput.of("context", ctx(), "chunks", chunks(4, 4000), "materialId", "m1",
                "title", "w5.pdf", "options", new SummaryOptions(SummaryLength.BRIEF), "mapReduce", true));
        assertTrue(r.success);
        Summary s = r.payload();
        assertEquals(List.of("k1", "k2"), s.getKeyPoints());
        assertEquals(5, log.size(), "4 map calls + 1 reduce");
        assertTrue(log.get(4).userText().contains("Section 4 (p.4)"));
    }

    @Test
    void summarizationFailsWithoutPartialResultWhenLlmDown() {
        SummarizationTool tool = new SummarizationTool(new UnavailableLLMClient("down"), new ContextBuilder(1000), new ResponseValidator());
        ToolResult r = tool.execute(ToolInput.of("context", ctx(), "chunks", chunks(1, 10), "materialId", "m1", "title", "t"));
        assertFalse(r.success);
        assertNull(r.payload);
    }

    @Test
    void conceptExtractionMergesKnownConceptsAndDropsBadRelations() {
        String json = "{\"concepts\":[{\"name\":\"Inheritance\",\"definition\":\"is-a\"},{\"name\":\"Polymorphism\",\"definition\":\"many forms\"}],"
                + "\"relations\":[{\"from\":\"inheritance\",\"to\":\"Polymorphism\",\"type\":\"PREREQUISITE_OF\"},"
                + "{\"from\":\"Polymorphism\",\"to\":\"Unknown\",\"type\":\"RELATED_TO\"},"
                + "{\"from\":\"Polymorphism\",\"to\":\"Polymorphism\",\"type\":\"RELATED_TO\"}]}";
        ConceptExtractionTool tool = new ConceptExtractionTool(fake(r -> json, r -> "", new ArrayList<>()), new ContextBuilder(5000), new ResponseValidator());
        Concept known = new Concept("k-existing", "c1", "inheritance", "");
        ToolResult r = tool.execute(ToolInput.of("context", ctx(), "chunks", chunks(1, 50), "courseId", "c1",
                "materialId", "m1", "known", List.of(known)));
        ConceptGraph g = r.payload();
        assertEquals(2, g.nodes.size());
        assertEquals("k-existing", g.findByName("Inheritance").orElseThrow().getId(), "known concept reused");
        assertEquals(1, g.edges.size());
        assertTrue(r.message.contains("2 invalid relation(s) dropped"));
    }

    @Test
    void flashcardsDropDuplicatesAndReportShortfall() {
        String json = "{\"cards\":[{\"front\":\"What is a subclass?\",\"back\":\"A class that extends another\",\"conceptName\":\"Inheritance\"},"
                + "{\"front\":\"what is a subclass?\",\"back\":\"dup\"},{\"front\":\"Empty back\",\"back\":\"\"}]}";
        FlashcardGenerationTool tool = new FlashcardGenerationTool(fake(r -> json, r -> "", new ArrayList<>()), new ContextBuilder(5000), new ResponseValidator());
        ToolResult r = tool.execute(ToolInput.of("context", ctx(), "count", 3, "topic", "inheritance", "courseId", "c1",
                "concepts", List.of(new Concept("k1", "c1", "Inheritance", ""))));
        FlashcardDeck deck = r.payload();
        assertEquals(1, deck.getCards().size());
        assertEquals("k1", deck.getCards().get(0).getConceptId());
        assertNotNull(deck.getNotice());
    }
}
