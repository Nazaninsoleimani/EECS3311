package ca.yorku.eecs3311.studyagent.persistence;

import ca.yorku.eecs3311.studyagent.domain.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository {
    void save(Reminder r);
    Optional<Reminder> findById(String id);
    List<Reminder> findByStudent(String studentId);
    List<Reminder> findDue(LocalDateTime now);
}
