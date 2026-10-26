package ca.yorku.eecs3311.studyagent.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LearningHistory {
    private final List<LearningEvent> events = new ArrayList<>();

    public LearningHistory(List<LearningEvent> events) {
        this.events.addAll(events);
    }

    /** Matching events, newest first. */
    public List<LearningEvent> filter(HistoryFilter f) {
        return events.stream().filter(f::matches)
                .sorted(Comparator.comparing(LearningEvent::getTimestamp).reversed())
                .toList();
    }
}
