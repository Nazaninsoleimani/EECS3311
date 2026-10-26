package ca.yorku.eecs3311.studyagent.persistence.sqlite;

import ca.yorku.eecs3311.studyagent.domain.Student;
import ca.yorku.eecs3311.studyagent.persistence.StudentRepository;
import java.util.Optional;

public class SqliteStudentRepository implements StudentRepository {
    private final SqliteDatabase db;

    public SqliteStudentRepository(SqliteDatabase db) { this.db = db; }

    @Override public Optional<Student> findById(String id) { return db.get("student", id, Student.class); }
    @Override public void save(Student s) { db.put("student", s.getId(), null, s.getId(), s); }
}
