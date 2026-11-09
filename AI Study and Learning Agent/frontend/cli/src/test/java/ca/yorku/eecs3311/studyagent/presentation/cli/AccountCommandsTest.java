package ca.yorku.eecs3311.studyagent.presentation.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AccountCommandsTest {
    @TempDir Path dir;
    private final CliTestSupport t = new CliTestSupport();
    private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

    @AfterEach
    void close() { t.close(); }

    /** A fresh CLI process sharing the same database and session file. */
    private String run(String line, String input) {
        buf.reset();
        CLIApplication cli = new CLIApplication(t.app.facade(), t.app.studentId(), dir.resolve("cli-session"),
                new BufferedReader(new StringReader(input)), new PrintStream(buf, true, StandardCharsets.UTF_8));
        cli.run(CommandParser.tokenize(line));
        return buf.toString(StandardCharsets.UTF_8);
    }

    @Test
    void accountIsRememberedBetweenCommandsAndSharesCoursesWithTheGui() {
        assertTrue(run("account whoami", "").contains("Not signed in"));
        String created = run("account create", "Nazanin\nnazanin@example.com\nstudyhard1\nstudyhard1\n");
        assertTrue(created.contains("Account created. Signed in as Nazanin"), created);
        assertTrue(run("account whoami", "").contains("Signed in as Nazanin <nazanin@example.com>"));

        run("course create --code EECS3311 --title Design", "");
        String id = t.app.facade().signIn("nazanin@example.com", "studyhard1".toCharArray()).getId();
        assertEquals(1, t.app.facade().listCourses(id).size(), "course belongs to the signed-in account");
        assertTrue(t.app.facade().listCourses(t.app.studentId()).isEmpty(), "not to the local profile");

        assertTrue(run("account logout", "").contains("Signed out."));
        assertTrue(run("course list", "").contains("No courses yet"));
        assertTrue(run("account login --email nazanin@example.com", "wrong-pass\n").contains("Email or password is incorrect"));
        assertTrue(run("account login", "nazanin@example.com\nstudyhard1\n").contains("Your courses: 1"));
        assertTrue(run("course list", "").contains("EECS3311"));
    }

    @Test
    void invalidRegistrationIsRejected() {
        assertTrue(run("account create", "A\nnot-an-email\npassword1\npassword1\n").contains("Enter a valid email address"));
        assertTrue(run("account create", "A\na@b.co\nshort\nshort\n").contains("at least 8"));
    }
}
