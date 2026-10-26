package ca.yorku.eecs3311.studyagent.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CourseTest {

    @Test
    void codeIsNormalisedAndDaysUntilExamComputed() {
        Course c = new Course("c1", "s1", " eecs3311 ", "Software Design", LocalDate.of(2026, 12, 12));
        assertEquals("EECS3311", c.getCode());
        assertEquals(10, c.daysUntilExam(LocalDate.of(2026, 12, 2)));
    }

    @Test
    void noExamDateGivesMinusOne() {
        Course c = new Course("c1", "s1", "EECS2030", "Advanced OOP", null);
        assertEquals(-1, c.daysUntilExam());
    }

    @Test
    void blankCodeRejected() {
        assertThrows(ValidationException.class, () -> new Course("c1", "s1", " ", "x", null));
    }

    @Test
    void fileTypeDetectedFromExtension() {
        assertEquals(FileType.PDF, FileType.fromPath(Path.of("week5.PDF")));
        assertEquals(FileType.MARKDOWN, FileType.fromPath(Path.of("notes.md")));
        assertEquals(FileType.UNKNOWN, FileType.fromPath(Path.of("slides.pptx")));
    }
}
