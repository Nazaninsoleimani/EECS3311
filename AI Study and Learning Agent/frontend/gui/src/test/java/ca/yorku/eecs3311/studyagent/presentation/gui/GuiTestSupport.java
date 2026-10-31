package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.config.AppConfig;
import ca.yorku.eecs3311.studyagent.config.AppContext;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.CourseMaterial;
import ca.yorku.eecs3311.studyagent.integration.llm.*;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteDatabase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

/** In-memory backend with a scripted LLM for building views headlessly. */
class GuiTestSupport implements AutoCloseable {
    static final String CONCEPTS = "{\"concepts\":[{\"name\":\"Observer Pattern\",\"definition\":\"subjects notify observers\"},"
            + "{\"name\":\"Adapter Pattern\",\"definition\":\"converts an interface\"}],"
            + "\"relations\":[{\"from\":\"Adapter Pattern\",\"to\":\"Observer Pattern\",\"type\":\"RELATED_TO\"}]}";
    static final String QUIZ = "{\"questions\":["
            + "{\"type\":\"TRUE_FALSE\",\"prompt\":\"A subject notifies its observers.\",\"answer\":\"true\",\"conceptName\":\"Observer Pattern\"},"
            + "{\"type\":\"MULTIPLE_CHOICE\",\"prompt\":\"What does an adapter convert?\",\"options\":[\"An interface\",\"A database\",\"A thread\"],"
            + "\"answer\":\"A\",\"conceptName\":\"Adapter Pattern\"}]}";
    static final String CARDS = "{\"cards\":[{\"front\":\"What does a subject do?\",\"back\":\"Notifies observers\",\"conceptName\":\"Observer Pattern\"}]}";

    final AppContext app;
    final GuiContext ctx;

    GuiTestSupport() {
        Map<String, String> bySchema = Map.of("concepts", CONCEPTS, "quiz", QUIZ, "flashcards", CARDS,
                "summary", "{\"summary\":\"Observer and adapter.\",\"keyPoints\":[\"Subjects notify\"]}");
        LLMClient llm = new LLMClient() {
            public LLMResponse generate(LLMRequest r) { return new LLMResponse("A subject notifies its observers [1].", 3, "STOP"); }
            public LLMResponse generateStructured(LLMRequest r, OutputSchema s) { return new LLMResponse(bySchema.getOrDefault(s.name, "{}"), 3, "STOP"); }
            public String getModelName() { return "scripted"; }
        };
        app = AppContext.create(new AppConfig(Map.of()), SqliteDatabase.inMemory(), llm, (t, b, to) -> true);
        ctx = new GuiContext(app.facade(), app.studentId());
    }

    /** A course with one indexed material and extracted concepts, selected in the context. */
    Course courseWithMaterial(Path dir) throws Exception {
        Course c = app.facade().createCourse(app.studentId(), "EECS3311", "Software Design", LocalDate.now().plusDays(6));
        Path notes = Files.writeString(dir.resolve("patterns.md"),
                "The observer pattern: a subject notifies its observers. ".repeat(20) + "An adapter converts an interface. ".repeat(20));
        CourseMaterial m = app.facade().uploadMaterial(c.getId(), notes);
        app.facade().extractConcepts(m.getId());
        ctx.reloadCourses();
        return c;
    }

    @Override
    public void close() {
        app.close();
    }
}
