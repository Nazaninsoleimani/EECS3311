package ca.yorku.eecs3311.studyagent.persistence.sqlite;

import ca.yorku.eecs3311.studyagent.domain.Reminder;
import ca.yorku.eecs3311.studyagent.persistence.ReminderRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SqliteReminderRepository implements ReminderRepository {
    private final SqliteDatabase db;

    public SqliteReminderRepository(SqliteDatabase db) { this.db = db; }

    @Override public void save(Reminder r) { db.put("reminder", r.getId(), r.getCourseId(), r.getStudentId(), r); }
    @Override public Optional<Reminder> findById(String id) { return db.get("reminder", id, Reminder.class); }

    @Override
    public List<Reminder> findByStudent(String studentId) {
        return db.byOwner("reminder", studentId, Reminder.class).stream()
                .sorted(Comparator.comparing(Reminder::getDueAt)).toList();
    }

    @Override
    public List<Reminder> findDue(LocalDateTime now) {
        return db.all("reminder", Reminder.class).stream().filter(r -> r.isDue(now)).toList();
    }
}
