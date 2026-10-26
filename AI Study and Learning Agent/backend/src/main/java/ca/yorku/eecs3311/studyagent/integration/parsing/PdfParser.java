package ca.yorku.eecs3311.studyagent.integration.parsing;

import ca.yorku.eecs3311.studyagent.domain.FileType;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/** Text-based PDFs via Apache PDFBox, one text block per page so chunks keep page references. */
public class PdfParser implements DocumentParser {
    @Override
    public boolean supports(FileType type) {
        return type == FileType.PDF;
    }

    @Override
    public ParsedDocument parse(Path file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<String> pages = new ArrayList<>();
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                pages.add(stripper.getText(doc));
            }
            return new ParsedDocument(pages, true);
        }
    }
}
