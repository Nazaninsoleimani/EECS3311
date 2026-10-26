package ca.yorku.eecs3311.studyagent.integration.parsing;

import ca.yorku.eecs3311.studyagent.domain.FileType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Word documents: reads word/document.xml from the DOCX zip and keeps paragraph breaks. */
public class DocxParser implements DocumentParser {
    @Override
    public boolean supports(FileType type) {
        return type == FileType.DOCX;
    }

    @Override
    public ParsedDocument parse(Path file) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry entry = zip.getEntry("word/document.xml");
            if (entry == null) throw new IOException("Not a valid DOCX file: missing word/document.xml");
            try (InputStream in = zip.getInputStream(entry)) {
                String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return new ParsedDocument(List.of(xmlToText(xml)), false);
            }
        }
    }

    public static String xmlToText(String xml) {
        String text = xml.replaceAll("</w:p>", "\n")
                .replaceAll("<w:tab/>", "\t")
                .replaceAll("<w:br/>", "\n")
                .replaceAll("<[^>]+>", "");
        return text.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'").replace("&amp;", "&").trim();
    }
}
