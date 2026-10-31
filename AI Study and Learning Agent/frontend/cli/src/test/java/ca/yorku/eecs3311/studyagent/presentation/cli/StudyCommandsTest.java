package ca.yorku.eecs3311.studyagent.presentation.cli;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.domain.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StudyCommandsTest {
    @TempDir Path dir;
    private final CliTestSupport t = new CliTestSupport();

    @BeforeEach
    void courseWithConcepts() throws Exception {
        t.run("course create --code EECS3311 --title Design --exam " + LocalDate.now().plusDays(5));
        t.output();
        Path notes = Files.writeString(dir.resolve("patterns.md"),
                "The observer pattern: a subject notifies its observers. ".repeat(20) + "An adapter converts an interface. ".repeat(20));
        t.run("material upload --course EECS3311 \"" + notes + "\"");
        String out = t.output();
        t.run("material extract-concepts " + out.substring(out.indexOf("(id ") + 4, out.indexOf(")")));
        t.output();
    }

    @AfterEach
    void close() { t.close(); }

    @Test
    void quizTakeGradesAndExplains() {
        assertEquals(0, t.run("quiz generate --course EECS3311 --count 2 --types tf,mcq --take", "true\nC\n"));
        String out = t.output();
        assertTrue(out.contains("Q2 [multiple choice, intermediate, Adapter Pattern] What does an adapter convert?\n   A) An interface"), out);
        assertTrue(out.contains("Score: 50% (1 of 2 correct)"), out);
        String attempt = out.substring(out.indexOf("Attempt ") + 8, out.indexOf(". Explain"));
        assertEquals(0, t.run("quiz explain " + attempt + " 2"));
        assertTrue(t.output().contains("Sources:"));
        assertEquals(1, t.run("quiz explain " + attempt + " 9"));
        assertTrue(t.output().contains("Question number must be 1-2"));
        assertEquals(1, t.run("quiz generate --course EECS3311 --types essay"));
        t.output();

        assertEquals(0, t.run("progress show --course EECS3311"));
        out = t.output();
        assertTrue(out.contains("quiz average 50%"), out);
        assertTrue(out.contains("Adapter Pattern"));
    }

    @Test
    void studyPlanGenerateShowAndComplete() {
        assertEquals(0, t.run("study-plan generate --course EECS3311 --minutes 60"));
        String out = t.output();
        assertTrue(out.startsWith("Exam cram plan"), out);
        String taskId = out.substring(out.indexOf("[ ] ") + 4, out.indexOf("[ ] ") + 14).trim();
        assertEquals(0, t.run("study-plan done " + taskId));
        assertTrue(t.output().startsWith("Done: "));
        t.run("study-plan show --course EECS3311 --all");
        assertTrue(t.output().contains("[x] " + taskId));
    }

    @Test
    void flashcardsGenerateAndReview() {
        assertEquals(0, t.run("flashcards generate --course EECS3311 --topic observer --count 2"));
        assertTrue(t.output().contains("Q: What does a subject do?"));
        assertEquals(0, t.run("flashcards review --course EECS3311", "\nmaybe\n3\n\n1\n"));
        String out = t.output();
        assertTrue(out.contains("Type 1, 2, 3, 4"), out);
        assertTrue(out.contains("box 2"));
        assertTrue(out.contains("Reviewed 2 card(s)."));
        t.run("flashcards list --course EECS3311");
        assertTrue(t.output().contains("2 cards, 0 due"), "both reviewed today; 'again' resets to box 1, due tomorrow");
    }

    @Test
    void learningSessionAndWeakTopics() {
        assertEquals(0, t.run("learning-session start --course EECS3311", "true\nfalse\nq\n"));
        String out = t.output();
        assertTrue(out.contains("at FOUNDATION level"), out);
        assertTrue(out.contains("Session finished: 1 of 2 correct"), out);
        assertEquals(0, t.run("weak-topics show --course EECS3311"));
        assertTrue(t.output().contains("Not enough graded practice"));
    }

    @Test
    void remindersConceptMapAndHistory() {
        String at = LocalDate.now().plusDays(1) + " 09:00";
        assertEquals(0, t.run("reminder add --at \"" + at + "\" --repeat daily --course EECS3311 \"Review adapters\""));
        String out = t.output();
        String id = out.substring(out.indexOf("Reminder ") + 9, out.indexOf(" set for"));
        t.run("reminder list");
        assertTrue(t.output().contains("DAILY   Review adapters"));
        assertEquals(0, t.run("reminder cancel " + id));
        t.run("reminder list");
        assertTrue(t.output().contains("No reminders."));
        assertEquals(1, t.run("reminder add --at tomorrow hi"));
        assertTrue(t.output().contains("--at must look like"));

        assertEquals(0, t.run("concept-map show --course EECS3311 --format mermaid"));
        assertTrue(t.output().startsWith("graph TD"));
        assertEquals(0, t.run("history show --course EECS3311 --last 1d --type concepts_extracted,reminder_created"));
        out = t.output();
        assertTrue(out.contains("CONCEPTS_EXTRACTED") && out.contains("REMINDER_CREATED") && !out.contains("MATERIAL_UPLOADED"), out);
    }

    @Test
    void helpListsEveryFeatureCommand() {
        t.run("help");
        String out = t.output();
        for (String c : new String[]{"course create", "material upload", "material summarize", "material extract-concepts", "ask",
                "study-plan generate", "flashcards review", "quiz generate", "quiz take", "quiz explain", "weak-topics show",
                "learning-session start", "progress show", "reminder add", "concept-map show", "history show"}) {
            assertTrue(out.contains("study " + c), c);
        }
    }
}
