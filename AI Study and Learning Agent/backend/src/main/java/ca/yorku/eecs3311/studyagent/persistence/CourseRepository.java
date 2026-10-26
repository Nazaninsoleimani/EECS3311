package ca.yorku.eecs3311.studyagent.persistence;

import ca.yorku.eecs3311.studyagent.domain.*;
import java.util.List;
import java.util.Optional;

public interface CourseRepository {
    void save(Course c);
    Optional<Course> findById(String id);
    List<Course> findByStudent(String studentId);
    void delete(String id);
}
