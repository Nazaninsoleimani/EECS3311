package ca.yorku.eecs3311.studyagent.domain;

import java.util.List;

public class Explanation {
    public final String text;
    public final List<String> sourceRefs;
    /** False when no course material backed the explanation. */
    public final boolean sourceBacked;

    public Explanation(String text, List<String> sourceRefs, boolean sourceBacked) {
        this.text = text;
        this.sourceRefs = List.copyOf(sourceRefs);
        this.sourceBacked = sourceBacked;
    }
}
