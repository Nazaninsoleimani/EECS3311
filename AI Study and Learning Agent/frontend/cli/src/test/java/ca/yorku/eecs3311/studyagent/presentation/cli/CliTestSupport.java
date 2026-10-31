package ca.yorku.eecs3311.studyagent.presentation.cli;

import ca.yorku.eecs3311.studyagent.config.AppConfig;
import ca.yorku.eecs3311.studyagent.config.AppContext;
import ca.yorku.eecs3311.studyagent.integration.llm.*;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteDatabase;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** In-memory backend with a scripted LLM, plus a CLI whose input and output are strings. */
class CliTestSupport implements AutoCloseable {
    static final String CONCEPTS = "{\"concepts\":[{\"name\":\"Observer Pattern\",\"definition\":\"subjects notify observers\"},"
            + "{\"name\":\"Adapter Pattern\",\"definition\":\"converts an interface\"}],\"relations\":[]}";
    static final String QUIZ = "{\"questions\":["
            + "{\"type\":\"TRUE_FALSE\",\"prompt\":\"A subject notifies its observers.\",\"answer\":\"true\",\"conceptName\":\"Observer Pattern\"},"
            + "{\"type\":\"MULTIPLE_CHOICE\",\"prompt\":\"What does an adapter convert?\",\"options\":[\"An interface\",\"A database\",\"A thread\"],"
            + "\"answer\":\"A\",\"conceptName\":\"Adapter Pattern\"}]}";
    static final String CARDS = "{\"cards\":[{\"front\":\"What does a subject do?\",\"back\":\"Notifies observers\",\"conceptName\":\"Observer Pattern\"},"
            + "{\"front\":\"What does an adapter do?\",\"back\":\"Converts an interface\",\"conceptName\":\"Adapter Pattern\"}]}";

    final AppContext app;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    CliTestSupport() {
        Map<String, String> bySchema = Map.of("concepts", CONCEPTS, "quiz", QUIZ, "flashcards", CARDS,
                "summary", "{\"summary\":\"Observer and adapter.\",\"keyPoints\":[\"Subjects notify\",\"Adapters convert\"]}");
        LLMClient llm = new LLMClient() {
            public LLMResponse generate(LLMRequest r) { return new LLMResponse("A subject notifies its observers [1].", 3, "STOP"); }
            public LLMResponse generateStructured(LLMRequest r, OutputSchema s) { return new LLMResponse(bySchema.getOrDefault(s.name, "{}"), 3, "STOP"); }
            public String getModelName() { return "scripted"; }
        };
        app = AppContext.create(new AppConfig(Map.of()), SqliteDatabase.inMemory(), llm, (t, b, to) -> true);
    }

    /** A CLI that reads the given lines as user input. */
    CLIApplication cli(String input) {
        return new CLIApplication(app.facade(), app.studentId(), new BufferedReader(new StringReader(input)),
                new PrintStream(buffer, true, StandardCharsets.UTF_8));
    }

    int run(String line) {
        return run(line, "");
    }

    int run(String line, String input) {
        return cli(input).run(CommandParser.tokenize(line));
    }

    /** Output since the last call. */
    String output() {
        String s = buffer.toString(StandardCharsets.UTF_8);
        buffer.reset();
        return s;
    }

    @Override
    public void close() {
        app.close();
    }
}
