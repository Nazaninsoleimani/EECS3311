package ca.yorku.eecs3311.studyagent.persistence;

import ca.yorku.eecs3311.studyagent.domain.*;
import java.util.List;
import java.util.Optional;

public interface StudentRepository {
    Optional<Student> findById(String id);
    void save(Student s);
}
