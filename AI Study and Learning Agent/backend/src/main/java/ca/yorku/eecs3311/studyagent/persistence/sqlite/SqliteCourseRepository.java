package ca.yorku.eecs3311.studyagent.persistence.sqlite;

import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.persistence.CourseRepository;
import java.util.List;
import java.util.Optional;

public class SqliteCourseRepository implements CourseRepository {
    private final SqliteDatabase db;

    public SqliteCourseRepository(SqliteDatabase db) { this.db = db; }

    @Override public void save(Course c) { db.put("course", c.getId(), null, c.getStudentId(), c); }
    @Override public Optional<Course> findById(String id) { return db.get("course", id, Course.class); }
    @Override public List<Course> findByStudent(String studentId) { return db.byOwner("course", studentId, Course.class); }
    @Override public void delete(String id) { db.delete("course", id); }
}
