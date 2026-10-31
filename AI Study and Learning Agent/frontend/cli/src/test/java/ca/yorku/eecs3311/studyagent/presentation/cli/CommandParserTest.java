package ca.yorku.eecs3311.studyagent.presentation.cli;

import static org.junit.jupiter.api.Assertions.*;

import ca.yorku.eecs3311.studyagent.domain.QuestionType;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandParserTest {
    private final CommandParser parser = new CommandParser();

    @Test
    void parsesGroupActionOptionsFlagsAndArguments() {
        ParsedCommand c = parser.parse(new String[]{"quiz", "generate", "--course", "EECS3311", "--count=5", "--trace",
                "--types", "mcq,tf", "extra"});
        assertEquals("quiz", c.group);
        assertEquals("generate", c.action);
        assertEquals("EECS3311", c.require("course"));
        assertEquals(5, c.intOption("count", 1));
        assertTrue(c.has("trace"));
        assertEquals(List.of("mcq", "tf"), c.listOption("types"));
        assertEquals(List.of("extra"), c.args);
    }

    @Test
    void singleWordCommandsTurnTheActionBackIntoAnArgument() {
        ParsedCommand c = parser.parse(new String[]{"ask", "--course", "c1", "What", "is", "-i"}).withActionAsArgument();
        assertEquals("", c.action);
        assertEquals("What is", c.text("question"));
        assertTrue(c.has("interactive"));
    }

    @Test
    void emptyInputMeansHelpAndBadValuesFailClearly() {
        assertEquals("help", parser.parse(new String[0]).group);
        ParsedCommand c = parser.parse(new String[]{"x", "y", "--count", "many", "--exam", "12/15", "--types", "essay"});
        assertThrows(ValidationException.class, () -> c.intOption("count", 1));
        assertThrows(ValidationException.class, () -> c.dateOption("exam"));
        assertThrows(ValidationException.class, () -> c.enumOption("types", QuestionType.class, null));
        assertThrows(ParsedCommand.UsageException.class, () -> c.require("course"));
        assertEquals(QuestionType.MULTIPLE_CHOICE, ParsedCommand.parseEnum("multiple-choice", QuestionType.class, "type"));
    }

    @Test
    void tokenizerHonoursQuotes() {
        assertArrayEquals(new String[]{"course", "create", "--title", "Software Design", "--code", "EECS3311"},
                CommandParser.tokenize("course create --title \"Software Design\"  --code 'EECS3311'"));
        assertArrayEquals(new String[]{"ask", ""}, CommandParser.tokenize("ask \"\""));
    }
}
