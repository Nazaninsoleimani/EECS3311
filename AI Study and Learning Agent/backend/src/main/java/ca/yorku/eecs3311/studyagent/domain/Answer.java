package ca.yorku.eecs3311.studyagent.domain;

import java.util.List;

/** Tutor answer (F05) with citations to retrieved course material. */
public class Answer {
    public final String text;
    public final List<String> citations;
    public final boolean grounded;

    public Answer(String text, List<String> citations, boolean grounded) {
        this.text = text;
        this.citations = List.copyOf(citations);
        this.grounded = grounded;
    }
}
