package ca.yorku.eecs3311.studyagent.agent;

import ca.yorku.eecs3311.studyagent.domain.ConceptMastery;
import ca.yorku.eecs3311.studyagent.domain.LearningEvent;
import ca.yorku.eecs3311.studyagent.domain.StudentProfile;
import java.time.LocalDate;
import java.util.List;

/** Long-term memory snapshot the agent reasons over. */
public class LearnerContext {
    public final String studentId;
    public final String courseId;
    public final StudentProfile profile;
    public final List<ConceptMastery> masteries;
    public final List<LearningEvent> recentEvents;
    public final LocalDate examDate;

    public LearnerContext(String studentId, String courseId, StudentProfile profile, List<ConceptMastery> masteries,
                          List<LearningEvent> recentEvents, LocalDate examDate) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.profile = profile;
        this.masteries = List.copyOf(masteries);
        this.recentEvents = List.copyOf(recentEvents);
        this.examDate = examDate;
    }

    /** Compact text summary included in prompts. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        if (examDate != null) sb.append("Exam date: ").append(examDate).append('\n');
        if (masteries.isEmpty()) {
            sb.append("No graded practice yet.\n");
        } else {
            sb.append("Concept mastery (0-1):\n");
            masteries.stream().limit(25).forEach(m -> sb.append(String.format("- %s: %.2f (%d attempts)%n",
                    m.getConceptName(), m.getMastery(), m.getAttempts())));
        }
        if (!recentEvents.isEmpty()) {
            sb.append("Recent activity:\n");
            recentEvents.stream().limit(5).forEach(e -> sb.append("- ").append(e.getType()).append(": ").append(e.getSummary()).append('\n'));
        }
        return sb.toString();
    }
}
