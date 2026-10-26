package ca.yorku.eecs3311.studyagent.integration.parsing;

import ca.yorku.eecs3311.studyagent.domain.DocumentChunk;
import ca.yorku.eecs3311.studyagent.domain.Ids;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits text into overlapping chunks. Sizes are in tokens and approximated as words / 0.75,
 * so the default 800-token chunk with 100-token overlap is about 600 words with 75 overlapping.
 */
public class Chunker {
    private final int chunkSize;
    private final int overlap;

    public Chunker() {
        this(800, 100);
    }

    public Chunker(int chunkSizeTokens, int overlapTokens) {
        if (overlapTokens >= chunkSizeTokens) throw new IllegalArgumentException("overlap must be smaller than chunk size");
        this.chunkSize = Math.max(1, (int) Math.round(chunkSizeTokens * 0.75));
        this.overlap = (int) Math.round(overlapTokens * 0.75);
    }

    public List<DocumentChunk> chunk(ParsedDocument doc, String materialId, String courseId, String materialTitle) {
        List<String> words = new ArrayList<>();
        List<Integer> pageOfWord = new ArrayList<>();
        for (int p = 0; p < doc.pageTexts.size(); p++) {
            for (String w : doc.pageTexts.get(p).split("\\s+")) {
                if (!w.isBlank()) {
                    words.add(w);
                    pageOfWord.add(p + 1);
                }
            }
        }
        List<DocumentChunk> chunks = new ArrayList<>();
        int step = chunkSize - overlap;
        for (int start = 0, index = 0; start < words.size(); start += step, index++) {
            int end = Math.min(words.size(), start + chunkSize);
            String text = String.join(" ", words.subList(start, end));
            String pageRef = null;
            if (doc.paged) {
                int first = pageOfWord.get(start);
                int last = pageOfWord.get(end - 1);
                pageRef = first == last ? "p." + first : "pp." + first + "-" + last;
            }
            chunks.add(new DocumentChunk(Ids.newId("ch"), materialId, courseId, materialTitle, index, text, pageRef));
            if (end == words.size()) break;
        }
        return chunks;
    }
}
