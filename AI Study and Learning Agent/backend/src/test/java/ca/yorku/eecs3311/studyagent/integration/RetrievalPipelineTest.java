package ca.yorku.eecs3311.studyagent.integration;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.domain.*;
import ca.yorku.eecs3311.studyagent.integration.parsing.*;
import ca.yorku.eecs3311.studyagent.integration.retrieval.*;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteDatabase;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteMaterialRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RetrievalPipelineTest {

    @Test
    void chunkerOverlapsAndKeepsPageRefs() {
        String page1 = "alpha ".repeat(500);
        String page2 = "beta ".repeat(500);
        List<DocumentChunk> chunks = new Chunker(800, 100).chunk(new ParsedDocument(List.of(page1, page2), true), "m1", "c1", "w5.pdf");
        assertEquals(2, chunks.size());
        assertEquals("pp.1-2", chunks.get(0).getPageRef());
        assertEquals("p.2", chunks.get(1).getPageRef());
        assertEquals("w5.pdf p.2", chunks.get(1).sourceLabel());
    }

    @Test
    void docxXmlConvertedToText() {
        String xml = "<w:document><w:body><w:p><w:r><w:t>Inheritance &amp; reuse</w:t></w:r></w:p><w:p><w:t>Line 2</w:t></w:p></w:body></w:document>";
        assertEquals("Inheritance & reuse\nLine 2", DocxParser.xmlToText(xml));
    }

    @Test
    void retrievalFindsRelevantChunkAndRespectsThreshold(@TempDir Path dir) throws Exception {
        Path notes = dir.resolve("oop.md");
        Files.writeString(notes, "");
        try (SqliteDatabase db = SqliteDatabase.inMemory()) {
            SqliteMaterialRepository materials = new SqliteMaterialRepository(db);
            RetrievalService retrieval = new RetrievalService(new LocalHashEmbeddingService(), new SqliteVectorStore(db, materials));
            CourseMaterial m = new CourseMaterial("m1", "c1", "oop.md", FileType.MARKDOWN, notes.toString());
            List<DocumentChunk> chunks = List.of(
                    new DocumentChunk("ch1", "m1", "c1", "oop.md", 0,
                            "Polymorphism lets a superclass reference call an overridden method; dynamic dispatch selects the subclass method at runtime.", null),
                    new DocumentChunk("ch2", "m1", "c1", "oop.md", 1,
                            "A Java interface declares abstract methods that implementing classes must provide.", null));
            materials.saveChunks("m1", chunks);
            retrieval.index(m, chunks);

            List<RetrievedChunk> hits = retrieval.retrieve("How does dynamic dispatch choose the overridden method?", "c1", 2);
            assertFalse(hits.isEmpty());
            assertEquals("ch1", hits.get(0).chunk.getId());
            assertTrue(retrieval.retrieve("photosynthesis chlorophyll sunlight", "c1", 2).isEmpty());
            assertTrue(retrieval.retrieve("dynamic dispatch", "other-course", 2).isEmpty());

            retrieval.removeMaterial("m1");
            assertTrue(retrieval.retrieve("dynamic dispatch", "c1", 2).isEmpty());
        }
    }

    @Test
    void plainTextParserSupportsMarkdown(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("a.txt");
        Files.writeString(f, "hello world");
        PlainTextParser p = new PlainTextParser();
        assertTrue(p.supports(FileType.MARKDOWN));
        assertFalse(p.supports(FileType.PDF));
        assertEquals("hello world", p.parse(f).text);
    }
}
