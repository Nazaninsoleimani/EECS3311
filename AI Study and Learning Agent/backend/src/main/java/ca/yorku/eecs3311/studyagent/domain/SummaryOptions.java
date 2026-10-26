package ca.yorku.eecs3311.studyagent.domain;

public class SummaryOptions {
    public final SummaryLength length;

    public SummaryOptions(SummaryLength length) {
        this.length = length == null ? SummaryLength.STANDARD : length;
    }

    /** Approximate number of words the LLM is asked to write. */
    public int targetWords() {
        return switch (length) {
            case BRIEF -> 120;
            case STANDARD -> 300;
            case DETAILED -> 700;
        };
    }
}
