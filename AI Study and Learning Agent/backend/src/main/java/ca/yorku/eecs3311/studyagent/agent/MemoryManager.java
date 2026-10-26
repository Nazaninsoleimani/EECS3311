package ca.yorku.eecs3311.studyagent.agent;

import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.HistoryFilter;
import ca.yorku.eecs3311.studyagent.domain.LearningEvent;
import ca.yorku.eecs3311.studyagent.domain.StudentProfile;
import ca.yorku.eecs3311.studyagent.persistence.CourseRepository;
import ca.yorku.eecs3311.studyagent.persistence.LearningHistoryRepository;
import ca.yorku.eecs3311.studyagent.persistence.ProgressRepository;
import ca.yorku.eecs3311.studyagent.persistence.StudentRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Short-term (session) and long-term (history, mastery) memory of the agent. */
public class MemoryManager {
    private final Map<String, SessionMemory> sessions = new ConcurrentHashMap<>();
    private final StudentRepository students;
    private final CourseRepository courses;
    private final ProgressRepository progress;
    private final LearningHistoryRepository history;
    private final int maxTurns;

    public MemoryManager(StudentRepository students, CourseRepository courses, ProgressRepository progress,
                         LearningHistoryRepository history, int maxTurns) {
        this.students = students;
        this.courses = courses;
        this.progress = progress;
        this.history = history;
        this.maxTurns = maxTurns;
    }

    public LearnerContext loadLearnerContext(String studentId, String courseId) {
        StudentProfile profile = students.findById(studentId).map(s -> s.getProfile()).orElse(new StudentProfile());
        LocalDate exam = courseId == null ? null : courses.findById(courseId).map(Course::getExamDate).orElse(null);
        List<LearningEvent> recent = history.find(studentId,
                new HistoryFilter(courseId, LocalDate.now().minusDays(14), null, null)).stream().limit(10).toList();
        return new LearnerContext(studentId, courseId, profile,
                courseId == null ? List.of() : progress.findMastery(studentId, courseId), recent, exam);
    }

    public SessionMemory getSessionMemory(String sessionId) {
        return sessions.computeIfAbsent(sessionId, id -> new SessionMemory(id, maxTurns));
    }

    public void appendToSession(String sessionId, ConversationTurn turn) {
        getSessionMemory(sessionId).add(turn);
    }

    public void recordEvent(LearningEvent event) {
        history.append(event);
    }

    public List<LearningEvent> getHistory(String studentId, HistoryFilter filter) {
        return history.find(studentId, filter);
    }
}
