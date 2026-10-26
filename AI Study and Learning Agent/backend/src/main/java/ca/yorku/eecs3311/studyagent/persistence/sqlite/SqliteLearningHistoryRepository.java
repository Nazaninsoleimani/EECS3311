package ca.yorku.eecs3311.studyagent.persistence.sqlite;

import ca.yorku.eecs3311.studyagent.domain.HistoryFilter;
import ca.yorku.eecs3311.studyagent.domain.LearningEvent;
import ca.yorku.eecs3311.studyagent.domain.LearningHistory;
import ca.yorku.eecs3311.studyagent.persistence.LearningHistoryRepository;
import java.util.List;

public class SqliteLearningHistoryRepository implements LearningHistoryRepository {
    private final SqliteDatabase db;

    public SqliteLearningHistoryRepository(SqliteDatabase db) { this.db = db; }

    @Override public void append(LearningEvent e) { db.put("event", e.getId(), e.getCourseId(), e.getStudentId(), e); }

    @Override
    public List<LearningEvent> find(String studentId, HistoryFilter filter) {
        return new LearningHistory(db.byOwner("event", studentId, LearningEvent.class)).filter(filter);
    }
}
