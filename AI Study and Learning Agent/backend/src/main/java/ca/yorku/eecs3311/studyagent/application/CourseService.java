package ca.yorku.eecs3311.studyagent.application;

import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.CourseMaterial;
import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import ca.yorku.eecs3311.studyagent.integration.retrieval.RetrievalService;
import ca.yorku.eecs3311.studyagent.persistence.ConceptRepository;
import ca.yorku.eecs3311.studyagent.persistence.CourseRepository;
import ca.yorku.eecs3311.studyagent.persistence.FlashcardRepository;
import ca.yorku.eecs3311.studyagent.persistence.MaterialRepository;
import ca.yorku.eecs3311.studyagent.persistence.ProgressRepository;
import ca.yorku.eecs3311.studyagent.persistence.QuizRepository;
import ca.yorku.eecs3311.studyagent.persistence.StudyPlanRepository;
import java.time.LocalDate;
import java.util.List;

/** F01: course workspaces. Deleting a course removes everything that belongs to it. */
public class CourseService {
    private final CourseRepository courses;
    private final MaterialRepository materials;
    private final ConceptRepository concepts;
    private final ProgressRepository progress;
    private final QuizRepository quizzes;
    private final FlashcardRepository flashcards;
    private final StudyPlanRepository plans;
    private final RetrievalService retrieval;

    public CourseService(CourseRepository courses, MaterialRepository materials, ConceptRepository concepts,
                         ProgressRepository progress, QuizRepository quizzes, FlashcardRepository flashcards,
                         StudyPlanRepository plans, RetrievalService retrieval) {
        this.courses = courses;
        this.materials = materials;
        this.concepts = concepts;
        this.progress = progress;
        this.quizzes = quizzes;
        this.flashcards = flashcards;
        this.plans = plans;
        this.retrieval = retrieval;
    }

    public Course createCourse(String studentId, String code, String title, LocalDate examDate) {
        Course course = new Course(Ids.newId("c"), studentId, code, title, examDate);
        requireUniqueCode(studentId, course.getCode(), null);
        courses.save(course);
        return course;
    }

    public Course updateCourse(Course course) {
        find(course.getId());
        requireUniqueCode(course.getStudentId(), course.getCode(), course.getId());
        courses.save(course);
        return course;
    }

    public void deleteCourse(String courseId) {
        find(courseId);
        for (CourseMaterial m : materials.findByCourse(courseId)) materials.delete(m.getId());
        retrieval.removeCourse(courseId);
        concepts.deleteByCourse(courseId);
        progress.deleteByCourse(courseId);
        quizzes.deleteByCourse(courseId);
        flashcards.deleteByCourse(courseId);
        plans.deleteByCourse(courseId);
        courses.delete(courseId);
    }

    public List<Course> listCourses(String studentId) {
        return courses.findByStudent(studentId);
    }

    public Course find(String courseId) {
        return courses.findById(courseId).orElseThrow(() -> new ValidationException("No course with id " + courseId));
    }

    /** Accepts a course id or a course code such as EECS3311 (case-insensitive). */
    public Course resolve(String studentId, String idOrCode) {
        if (idOrCode == null || idOrCode.isBlank()) throw new ValidationException("A course is required (id or code)");
        return courses.findById(idOrCode).or(() -> listCourses(studentId).stream()
                        .filter(c -> c.getCode().equalsIgnoreCase(idOrCode.trim())).findFirst())
                .orElseThrow(() -> new ValidationException("No course '" + idOrCode + "'"));
    }

    private void requireUniqueCode(String studentId, String code, String exceptId) {
        boolean taken = courses.findByStudent(studentId).stream()
                .anyMatch(c -> c.getCode().equals(code) && !c.getId().equals(exceptId));
        if (taken) throw new ValidationException("You already have a course with code " + code);
    }
}
