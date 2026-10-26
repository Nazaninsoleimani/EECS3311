package ca.yorku.eecs3311.studyagent.integration.retrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Offline embedding: hashed bag of word unigrams and bigrams with log term frequency, L2-normalised.
 * Deterministic and free, so retrieval works without any API key; quality is keyword-level.
 */
public class LocalHashEmbeddingService implements EmbeddingService {
    private static final int DIM = 1024;
    private static final Set<String> STOP = Set.of("the", "a", "an", "of", "to", "and", "or", "in", "on", "is", "are",
            "was", "were", "be", "for", "with", "as", "by", "it", "this", "that", "at", "from", "what", "why", "how",
            "which", "can", "do", "does", "i", "you", "we", "they", "not", "if", "then", "so", "its", "into");

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> result = new ArrayList<>(texts.size());
        for (String t : texts) result.add(embedOne(t));
        return result;
    }

    float[] embedOne(String text) {
        float[] v = new float[DIM];
        List<String> tokens = tokens(text);
        for (int i = 0; i < tokens.size(); i++) {
            add(v, tokens.get(i), 1.0f);
            if (i + 1 < tokens.size()) add(v, tokens.get(i) + "_" + tokens.get(i + 1), 0.5f);
        }
        double norm = 0;
        for (int i = 0; i < DIM; i++) {
            if (v[i] != 0) v[i] = (float) (Math.signum(v[i]) * Math.log1p(Math.abs(v[i])));
            norm += v[i] * v[i];
        }
        if (norm > 0) {
            float inv = (float) (1.0 / Math.sqrt(norm));
            for (int i = 0; i < DIM; i++) v[i] *= inv;
        }
        return v;
    }

    private static void add(float[] v, String token, float weight) {
        int h = token.hashCode();
        v[Math.floorMod(h, DIM)] += (h & 0x40000000) == 0 ? weight : -weight;
    }

    static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        for (String w : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (w.length() < 2 || STOP.contains(w)) continue;
            out.add(stem(w));
        }
        return out;
    }

    /** Very light stemming so "classes"/"class" and "inherited"/"inherit" match. */
    private static String stem(String w) {
        if (w.length() > 5 && w.endsWith("ing")) return w.substring(0, w.length() - 3);
        if (w.length() > 4 && w.endsWith("ed")) return w.substring(0, w.length() - 2);
        if (w.length() > 4 && w.endsWith("es")) return w.substring(0, w.length() - 2);
        if (w.length() > 3 && w.endsWith("s") && !w.endsWith("ss")) return w.substring(0, w.length() - 1);
        return w;
    }

    @Override public String modelName() { return "local-hash-1024"; }
    @Override public double suggestedMinScore() { return 0.12; }
}
