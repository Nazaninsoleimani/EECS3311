package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDate;
import java.util.List;

public class HistoryFilter {
    public final String courseId;
    public final LocalDate from;
    public final LocalDate to;
    public final List<EventType> eventTypes;

    public HistoryFilter(String courseId, LocalDate from, LocalDate to, List<EventType> eventTypes) {
        this.courseId = courseId;
        this.from = from;
        this.to = to;
        this.eventTypes = eventTypes == null ? List.of() : List.copyOf(eventTypes);
    }

    public static HistoryFilter all() { return new HistoryFilter(null, null, null, null); }

    public boolean matches(LearningEvent e) {
        if (courseId != null && !courseId.equals(e.getCourseId())) return false;
        LocalDate day = e.getTimestamp().toLocalDate();
        if (from != null && day.isBefore(from)) return false;
        if (to != null && day.isAfter(to)) return false;
        return eventTypes.isEmpty() || eventTypes.contains(e.getType());
    }
}
