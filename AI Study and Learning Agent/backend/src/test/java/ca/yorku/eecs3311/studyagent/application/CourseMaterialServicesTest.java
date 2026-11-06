package ca.yorku.eecs3311.studyagent.application;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.domain.*;
import ca.yorku.eecs3311.studyagent.integration.parsing.*;
import ca.yorku.eecs3311.studyagent.integration.retrieval.*;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CourseMaterialServicesTest {
    @TempDir Path dir;

    private final SqliteDatabase db = SqliteDatabase.inMemory();
    private final SqliteMaterialRepository materials = new SqliteMaterialRepository(db);
    private final SqliteConceptRepository concepts = new SqliteConceptRepository(db);
    private final SqliteProgressRepository progress = new SqliteProgressRepository(db);
    private final RetrievalService retrieval = new RetrievalService(new LocalHashEmbeddingService(), new SqliteVectorStore(db, materials));
    private final CourseService courses = new CourseService(new SqliteCourseRepository(db), materials, concepts, progress,
            new SqliteQuizRepository(db), new SqliteFlashcardRepository(db), new SqliteStudyPlanRepository(db), retrieval);
    private final MaterialService service = new MaterialService(List.of(new PdfParser(), new DocxParser(), new PlainTextParser()),
            new Chunker(80, 10), materials, retrieval);

    @Test
    void courseCodesAreUniquePerStudentAndResolvable() {
        Course c = courses.createCourse("s1", "eecs3311", "Software Design", null);
        assertEquals("EECS3311", c.getCode());
        assertThrows(ValidationException.class, () -> courses.createCourse("s1", "EECS3311", "Again", null));
        assertDoesNotThrow(() -> courses.createCourse("s2", "EECS3311", "Other student", null));
        assertEquals(c.getId(), courses.resolve("s1", "Eecs3311").getId());
        assertEquals(c.getId(), courses.resolve("s1", c.getId()).getId());
    }

    @Test
    void pastExamDatesAreRejectedButAnUnchangedOneIsKept() {
        assertThrows(ValidationException.class, () -> courses.createCourse("s1", "EECS3311", "Design", LocalDate.now().minusDays(1)));
        Course c = courses.createCourse("s1", "EECS3311", "Design", LocalDate.now());
        c.setExamDate(LocalDate.now().minusDays(3));
        ValidationException e = assertThrows(ValidationException.class, () -> courses.updateCourse(c));
        assertTrue(e.getMessage().contains("in the past"));

        // a course whose exam already passed can still be renamed
        db.put("course", c.getId(), null, "s1", new Course(c.getId(), "s1", "EECS3311", "Design", LocalDate.now().minusDays(30)));
        Course old = courses.find(c.getId());
        old.setTitle("Design (done)");
        assertEquals("Design (done)", courses.updateCourse(old).getTitle());
    }

    @Test
    void uploadParsesChunksAndIndexesForRetrieval() throws Exception {
        Course c = courses.createCourse("s1", "EECS3311", "Software Design", null);
        Path notes = Files.writeString(dir.resolve("week3.md"),
                "The observer pattern lets subjects notify observers. ".repeat(40) + "Adapters convert interfaces. ".repeat(40));
        CourseMaterial m = service.upload(c.getId(), notes);
        assertTrue(m.isIndexed());
        assertTrue(m.getChunkCount() > 1);
        assertEquals(FileType.MARKDOWN, m.getFileType());
        assertFalse(retrieval.retrieve("observer notify subjects", c.getId(), 3).isEmpty());
    }

    @Test
    void emptyOrUnsupportedFilesAreRejectedClearly() throws Exception {
        Course c = courses.createCourse("s1", "EECS3311", "Software Design", null);
        CourseMaterial empty = service.upload(c.getId(), Files.writeString(dir.resolve("empty.txt"), "   "));
        assertEquals(MaterialStatus.FAILED, empty.getStatus());
        assertTrue(empty.getFailureReason().contains("No extractable text"));
        Path pptx = Files.writeString(dir.resolve("slides.pptx"), "x");
        ValidationException e = assertThrows(ValidationException.class, () -> service.upload(c.getId(), pptx));
        assertTrue(e.getMessage().contains("Unsupported"));
        assertThrows(ValidationException.class, () -> service.upload(c.getId(), dir.resolve("missing.pdf")));
        CourseMaterial broken = service.upload(c.getId(), Files.writeString(dir.resolve("broken.pdf"), "not a pdf"));
        assertEquals(MaterialStatus.FAILED, broken.getStatus());
    }

    @Test
    void embeddingOutageKeepsMaterialPendingUntilRetry() throws Exception {
        boolean[] down = {true};
        EmbeddingService local = new LocalHashEmbeddingService();
        EmbeddingService flaky = new EmbeddingService() {
            public List<float[]> embed(List<String> texts) {
                if (down[0]) throw new IllegalStateException("embedding API timeout");
                return local.embed(texts);
            }
            public String modelName() { return local.modelName(); }
            public double suggestedMinScore() { return local.suggestedMinScore(); }
        };
        RetrievalService flakyRetrieval = new RetrievalService(flaky, new SqliteVectorStore(db, materials));
        MaterialService svc = new MaterialService(List.of(new PlainTextParser()), new Chunker(80, 10), materials, flakyRetrieval);
        Course c = courses.createCourse("s1", "EECS3311", "Design", null);

        CourseMaterial m = svc.upload(c.getId(), Files.writeString(dir.resolve("n.txt"), "Strategy objects encapsulate algorithms. ".repeat(40)));
        assertEquals(MaterialStatus.UPLOADED, m.getStatus());
        assertTrue(m.isIndexingPending());
        assertTrue(m.getFailureReason().contains("embedding API timeout"));
        assertFalse(materials.findChunks(m.getId()).isEmpty(), "parsed text is kept");
        assertEquals(0, svc.retryPendingIndexing(List.of(c.getId())), "still down");

        down[0] = false;
        assertEquals(1, svc.retryPendingIndexing(List.of(c.getId())));
        CourseMaterial after = materials.findById(m.getId()).orElseThrow();
        assertTrue(after.isIndexed());
        assertNull(after.getFailureReason());
        assertFalse(flakyRetrieval.retrieve("strategy algorithms", c.getId(), 3).isEmpty());
        assertEquals(0, svc.retryPendingIndexing(List.of(c.getId())), "nothing left to retry");
    }

    @Test
    void deletingCourseRemovesMaterialsConceptsAndVectors() throws Exception {
        Course c = courses.createCourse("s1", "EECS3311", "Software Design", null);
        CourseMaterial m = service.upload(c.getId(), Files.writeString(dir.resolve("a.txt"), "Factory method creates products. ".repeat(30)));
        concepts.saveAll(c.getId(), List.of(new Concept("k1", c.getId(), "Factory Method", "")));
        courses.deleteCourse(c.getId());
        assertTrue(courses.listCourses("s1").isEmpty());
        assertTrue(materials.findById(m.getId()).isEmpty());
        assertTrue(concepts.findByCourse(c.getId()).isEmpty());
        assertTrue(retrieval.retrieve("factory method products", c.getId(), 3).isEmpty());
    }

    @Test
    void conceptMapRemapsRelationsToStoredIdsAndShowsMastery() {
        ConceptMapService maps = new ConceptMapService(concepts, progress);
        concepts.saveAll("c1", List.of(new Concept("k-old", "c1", "Inheritance", "")));
        ConceptGraph extracted = new ConceptGraph(
                List.of(new Concept("k-new", "c1", "inheritance", "is-a"), new Concept("k2", "c1", "Polymorphism", "")),
                List.of(new ConceptRelation("c1", "k-new", "k2", RelationType.PREREQUISITE_OF)));
        maps.save("c1", extracted);
        assertEquals("k-old", concepts.findRelations("c1").get(0).getFromId());

        ConceptMastery m = new ConceptMastery("s1", "c1", "k-old", "Inheritance");
        m.applyResult(new GradeResult("q", "k-old", true, 0.75, ""));
        progress.saveMastery(m);
        String text = ConceptMapService.renderText(maps.buildGraph("s1", "c1"));
        assertTrue(text.contains("Inheritance  (75% mastery)"));
        assertTrue(text.contains("--prerequisite_of--> Polymorphism"));
        assertTrue(text.contains("Polymorphism  (not practised)"));
    }
}
