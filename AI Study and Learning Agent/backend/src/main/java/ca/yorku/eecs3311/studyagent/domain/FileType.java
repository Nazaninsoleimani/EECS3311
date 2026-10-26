package ca.yorku.eecs3311.studyagent.domain;

import java.nio.file.Path;
import java.util.Locale;

/** File formats a material can have. UNKNOWN is rejected by MaterialService. */
public enum FileType {
    PDF, DOCX, TXT, MARKDOWN, UNKNOWN;

    public static FileType fromPath(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) return PDF;
        if (name.endsWith(".docx")) return DOCX;
        if (name.endsWith(".txt")) return TXT;
        if (name.endsWith(".md") || name.endsWith(".markdown")) return MARKDOWN;
        return UNKNOWN;
    }
}
