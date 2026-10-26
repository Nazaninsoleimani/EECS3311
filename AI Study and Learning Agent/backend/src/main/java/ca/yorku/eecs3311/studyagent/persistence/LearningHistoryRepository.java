package ca.yorku.eecs3311.studyagent.persistence;

import ca.yorku.eecs3311.studyagent.domain.*;
import java.util.List;
import java.util.Optional;

public interface LearningHistoryRepository {
    void append(LearningEvent event);
    List<LearningEvent> find(String studentId, HistoryFilter filter);
}
