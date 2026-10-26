package ca.yorku.eecs3311.studyagent.integration.parsing;

import ca.yorku.eecs3311.studyagent.domain.FileType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** TXT and Markdown files. */
public class PlainTextParser implements DocumentParser {
    @Override
    public boolean supports(FileType type) {
        return type == FileType.TXT || type == FileType.MARKDOWN;
    }

    @Override
    public ParsedDocument parse(Path file) throws IOException {
        return new ParsedDocument(List.of(Files.readString(file)), false);
    }
}
