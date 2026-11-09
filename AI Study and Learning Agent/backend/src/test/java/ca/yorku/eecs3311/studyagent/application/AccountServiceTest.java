package ca.yorku.eecs3311.studyagent.application;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.domain.Student;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteDatabase;
import ca.yorku.eecs3311.studyagent.persistence.sqlite.SqliteStudentRepository;
import org.junit.jupiter.api.Test;

class AccountServiceTest {
    private final SqliteStudentRepository students = new SqliteStudentRepository(SqliteDatabase.inMemory());
    private final AccountService accounts = new AccountService(students);

    private static char[] pw(String s) { return s.toCharArray(); }

    @Test
    void registerThenSignInWithAnyEmailCase() {
        Student s = accounts.register("Nazanin", " Nazanin@Example.com ", pw("secret123"), pw("secret123"));
        assertEquals("nazanin@example.com", s.getEmail());
        assertTrue(s.hasAccount());
        assertEquals(s.getId(), accounts.signIn("NAZANIN@example.com", pw("secret123")).getId());
        Student stored = students.findById(s.getId()).orElseThrow();
        assertNotNull(stored.getPasswordSalt());
        assertFalse(stored.getPasswordHash().contains("secret123"), "only a hash is stored");
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameMessage() {
        accounts.register("A", "a@b.co", pw("password1"), null);
        ValidationException wrong = assertThrows(ValidationException.class, () -> accounts.signIn("a@b.co", pw("password2")));
        ValidationException unknown = assertThrows(ValidationException.class, () -> accounts.signIn("x@b.co", pw("password1")));
        assertEquals(wrong.getMessage(), unknown.getMessage());
    }

    @Test
    void registrationIsValidated() {
        assertEquals("Enter your name", assertThrows(ValidationException.class,
                () -> accounts.register(" ", "a@b.co", pw("password1"), null)).getMessage());
        assertEquals("Enter a valid email address", assertThrows(ValidationException.class,
                () -> accounts.register("A", "not-an-email", pw("password1"), null)).getMessage());
        assertTrue(assertThrows(ValidationException.class,
                () -> accounts.register("A", "a@b.co", pw("short"), null)).getMessage().contains("at least 8"));
        assertEquals("Passwords do not match", assertThrows(ValidationException.class,
                () -> accounts.register("A", "a@b.co", pw("password1"), pw("password2"))).getMessage());
        accounts.register("A", "a@b.co", pw("password1"), null);
        assertEquals("An account with this email already exists", assertThrows(ValidationException.class,
                () -> accounts.register("B", "A@B.CO", pw("password9"), null)).getMessage());
    }

    @Test
    void localProfileWithoutPasswordCannotSignIn() {
        students.save(new Student("student-1", "Local", "local@example.com"));
        assertThrows(ValidationException.class, () -> accounts.signIn("local@example.com", pw("anything1")));
    }
}
