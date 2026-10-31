package ca.yorku.eecs3311.studyagent.presentation.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CourseMaterialCommandsTest {
    @TempDir Path dir;
    private final CliTestSupport t = new CliTestSupport();

    @AfterEach
    void close() { t.close(); }

    private String upload() throws Exception {
        Path notes = Files.writeString(dir.resolve("patterns.md"),
                "The observer pattern: a subject notifies its observers when its state changes. ".repeat(20));
        assertEquals(0, t.run("material upload --course EECS3311 \"" + notes + "\""));
        String out = t.output();
        assertTrue(out.contains("chunks indexed"), out);
        return out.substring(out.indexOf("(id ") + 4, out.indexOf(")"));
    }

    @Test
    void courseLifecycle() {
        assertEquals(0, t.run("course create --code eecs3311 --title \"Software Design\" --exam 2099-12-15"));
        assertTrue(t.output().contains("EECS3311 - Software Design"));
        assertEquals(1, t.run("course create --code EECS3311 --title Again"));
        assertTrue(t.output().contains("already have a course"));
        assertEquals(0, t.run("course update EECS3311 --title \"Design Patterns\" --exam none"));
        assertTrue(t.output().contains("Design Patterns"));
        assertEquals(0, t.run("course delete EECS3311", "n\n"));
        assertTrue(t.output().contains("Cancelled."));
        assertEquals(0, t.run("course delete EECS3311", "y\n"));
        t.output();
        t.run("course list");
        assertTrue(t.output().contains("No courses yet"));
    }

    @Test
    void uploadSummarizeExtractAndAsk() throws Exception {
        t.run("course create --code EECS3311 --title Design");
        String id = upload();
        assertEquals(0, t.run("material summarize " + id + " --length brief"));
        assertTrue(t.output().contains("Key points:\n  - Subjects notify"));
        assertEquals(0, t.run("material extract-concepts " + id));
        assertTrue(t.output().contains("2 concept(s)"));
        assertEquals(0, t.run("ask --course EECS3311 \"What does a subject do in the observer pattern?\" --trace"));
        String out = t.output();
        assertTrue(out.contains("Sources:\n  [1] patterns.md"), out);
        assertTrue(out.contains("--- agent trace ---"));
    }

    @Test
    void interactiveTutorKeepsAskingUntilEmptyLine() throws Exception {
        t.run("course create --code EECS3311 --title Design");
        upload();
        assertEquals(0, t.run("ask --course EECS3311 -i", "What does the observer pattern do?\nwhy is that?\n\n"));
        String out = t.output();
        assertEquals(2, out.split("Sources:").length - 1);
        assertTrue(out.endsWith("Bye.\n"));
    }

    @Test
    void usageErrorsAreReportedWithExitCodes() {
        assertEquals(2, t.run("course create --title X"));
        assertTrue(t.output().contains("Missing --code\nUsage: study course create"));
        assertEquals(2, t.run("teleport now"));
        assertTrue(t.output().contains("Unknown command"));
        assertEquals(1, t.run("material upload --course NOPE file.pdf"));
        assertTrue(t.output().contains("No course 'NOPE'"));
        assertEquals(0, t.run("help"));
        assertTrue(t.output().contains("study material summarize"));
        assertEquals(1, t.run("material summarize m-1 --length huge"));
        t.output();
    }
}
