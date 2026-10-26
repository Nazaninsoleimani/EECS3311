package ca.yorku.eecs3311.studyagent.integration.parsing;

import ca.yorku.eecs3311.studyagent.domain.FileType;
import java.io.IOException;
import java.nio.file.Path;

public interface DocumentParser {
    boolean supports(FileType type);

    ParsedDocument parse(Path file) throws IOException;
}
