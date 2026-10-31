package ca.yorku.eecs3311.studyagent.application;

import ca.yorku.eecs3311.studyagent.domain.CourseMaterial;
import ca.yorku.eecs3311.studyagent.domain.DocumentChunk;
import ca.yorku.eecs3311.studyagent.domain.FileType;
import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.Summary;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import ca.yorku.eecs3311.studyagent.integration.parsing.Chunker;
import ca.yorku.eecs3311.studyagent.integration.parsing.DocumentParser;
import ca.yorku.eecs3311.studyagent.integration.parsing.ParsedDocument;
import ca.yorku.eecs3311.studyagent.integration.retrieval.RetrievalService;
import ca.yorku.eecs3311.studyagent.persistence.MaterialRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * F02: upload pipeline parse -> chunk -> embed -> index. A material that cannot be parsed is kept
 * with status FAILED and a reason, so the student sees what went wrong.
 */
public class MaterialService {
    static final long MAX_BYTES = 50L * 1024 * 1024;

    private final List<DocumentParser> parsers;
    private final Chunker chunker;
    private final MaterialRepository materials;
    private final RetrievalService retrieval;

    public MaterialService(List<DocumentParser> parsers, Chunker chunker, MaterialRepository materials, RetrievalService retrieval) {
        this.parsers = List.copyOf(parsers);
        this.chunker = chunker;
        this.materials = materials;
        this.retrieval = retrieval;
    }

    public CourseMaterial upload(String courseId, Path file) {
        if (file == null || !Files.isRegularFile(file)) throw new ValidationException("File not found: " + file);
        FileType type = FileType.fromPath(file);
        DocumentParser parser = findParser(type);
        try {
            if (Files.size(file) > MAX_BYTES) throw new ValidationException("File is larger than 50 MB");
        } catch (IOException e) {
            throw new ValidationException("Cannot read " + file.getFileName() + ": " + e.getMessage());
        }
        String title = file.getFileName().toString();
        CourseMaterial material = new CourseMaterial(Ids.newId("m"), courseId, title, type, file.toAbsolutePath().toString());
        try {
            ParsedDocument doc = parser.parse(file);
            if (doc.isBlank()) {
                material.markFailed("No extractable text (scanned or image-only file?)");
            } else {
                List<DocumentChunk> chunks = chunker.chunk(doc, material.getId(), courseId, title);
                materials.saveChunks(material.getId(), chunks);
                retrieval.index(material, chunks);
                material.markIndexed(chunks.size());
            }
        } catch (IOException | RuntimeException e) {
            materials.delete(material.getId());
            material.markFailed("Could not process file: " + e.getMessage());
        }
        materials.save(material);
        return material;
    }

    public void delete(String materialId) {
        find(materialId);
        retrieval.removeMaterial(materialId);
        materials.delete(materialId);
    }

    public CourseMaterial find(String materialId) {
        return materials.findById(materialId).orElseThrow(() -> new ValidationException("No material with id " + materialId));
    }

    public List<CourseMaterial> list(String courseId) {
        return materials.findByCourse(courseId);
    }

    public void saveSummary(String materialId, Summary summary) {
        materials.saveSummary(materialId, summary);
    }

    private DocumentParser findParser(FileType type) {
        return parsers.stream().filter(p -> p.supports(type)).findFirst()
                .orElseThrow(() -> new ValidationException("Unsupported file type; use PDF, DOCX, TXT or Markdown"));
    }
}
