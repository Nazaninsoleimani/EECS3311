package ca.yorku.eecs3311.studyagent.integration.parsing;

import java.util.List;

/** Extracted text; pageTexts has one entry per page (a single entry for unpaged formats). */
public class ParsedDocument {
    public final String text;
    public final List<String> pageTexts;
    public final boolean paged;

    public ParsedDocument(List<String> pageTexts, boolean paged) {
        this.pageTexts = List.copyOf(pageTexts);
        this.text = String.join("\n\n", pageTexts);
        this.paged = paged;
    }

    public boolean isBlank() { return text.isBlank(); }
}
