package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.application.LearningFacade;
import ca.yorku.eecs3311.studyagent.domain.Course;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * State shared by all views: the facade, the local student, the selected course and the status
 * line. Views register listeners so that selecting a course or changing data refreshes them.
 */
public class GuiContext {
    final LearningFacade facade;
    final String studentId;
    private Course course;
    private List<Course> courses = List.of();
    private final List<Consumer<Course>> courseListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<List<Course>>> courseListListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> dataListeners = new CopyOnWriteArrayList<>();
    private Consumer<String> statusLine = s -> {};

    public GuiContext(LearningFacade facade, String studentId) {
        this.facade = facade;
        this.studentId = studentId;
    }

    public Course course() { return course; }

    public List<Course> courses() { return courses; }

    public void selectCourse(Course c) {
        if (c == course || c != null && course != null && c.getId().equals(course.getId())) {
            course = c;
            return;
        }
        course = c;
        courseListeners.forEach(l -> l.accept(c));
    }

    /** Reloads the course list (after create/update/delete) and keeps the selection if it still exists. */
    public void reloadCourses() {
        courses = facade.listCourses(studentId);
        courseListListeners.forEach(l -> l.accept(courses));
        Course keep = course == null ? null : courses.stream().filter(c -> c.getId().equals(course.getId())).findFirst().orElse(null);
        if (keep == null && !courses.isEmpty()) keep = courses.get(0);
        course = null;
        selectCourse(keep);
    }

    public void onCourseChanged(Consumer<Course> l) { courseListeners.add(l); }

    public void onCourseListChanged(Consumer<List<Course>> l) { courseListListeners.add(l); }

    /** Something other views may display changed (a quiz was graded, a task completed ...). */
    public void fireDataChanged() { dataListeners.forEach(Runnable::run); }

    public void onDataChanged(Runnable l) { dataListeners.add(l); }

    public void setStatusLine(Consumer<String> statusLine) { this.statusLine = statusLine; }

    public void status(String text) { statusLine.accept(text); }
}
