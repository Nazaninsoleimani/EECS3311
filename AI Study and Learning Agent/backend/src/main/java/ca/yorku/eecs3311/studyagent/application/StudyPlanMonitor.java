package ca.yorku.eecs3311.studyagent.application;

import ca.yorku.eecs3311.studyagent.agent.AgentException;
import ca.yorku.eecs3311.studyagent.agent.LearningAgent;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.EventType;
import ca.yorku.eecs3311.studyagent.domain.LearningEvent;
import ca.yorku.eecs3311.studyagent.domain.ProgressEvent;
import ca.yorku.eecs3311.studyagent.domain.StudyPlan;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import ca.yorku.eecs3311.studyagent.persistence.CourseRepository;
import ca.yorku.eecs3311.studyagent.persistence.LearningHistoryRepository;
import ca.yorku.eecs3311.studyagent.persistence.StudyPlanRepository;
import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * ProgressObserver that keeps the active study plan in step with performance (F06 adaptation):
 * when a concept's mastery drops sharply or falls below the weak threshold and the plan does not
 * revisit it within the next few days, the agent builds a revised plan and the old one is archived.
 */
public class StudyPlanMonitor implements ProgressObserver {
    static final int LOOKAHEAD_DAYS = 3;

    private final LearningAgent agent;
    private final StudyPlanRepository plans;
    private final CourseRepository courses;
    private final LearningHistoryRepository history;
    private Consumer<StudyPlan> onRevised = p -> {};

    public StudyPlanMonitor(LearningAgent agent, StudyPlanRepository plans, CourseRepository courses, LearningHistoryRepository history) {
        this.agent = agent;
        this.plans = plans;
        this.courses = courses;
        this.history = history;
    }

    /** Lets a front end show the student that their plan changed. */
    public void setOnRevised(Consumer<StudyPlan> listener) {
        this.onRevised = listener == null ? p -> {} : listener;
    }

    @Override
    public void onProgressChanged(ProgressEvent event) {
        StudyPlan plan = plans.findActive(event.studentId, event.courseId).orElse(null);
        if (plan == null || !needsRevision(plan, event, LocalDate.now())) return;
        LocalDate exam = courses.findById(event.courseId).map(Course::getExamDate).orElse(null);
        StudyPlan revised;
        try {
            revised = agent.revisePlan(plan, event, exam);
        } catch (AgentException | ValidationException e) {
            System.err.println("Study plan revision skipped: " + e.getMessage());
            return;
        }
        plan.archive();
        plans.save(plan);
        plans.save(revised);
        history.append(new LearningEvent(event.studentId, event.courseId, EventType.PLAN_REVISED,
                "Plan revised (revision " + revised.getRevision() + ") after " + event.conceptName + " dropped to "
                        + Math.round(event.newMastery * 100) + "%", revised.getId()));
        onRevised.accept(revised);
    }

    static boolean needsRevision(StudyPlan plan, ProgressEvent event, LocalDate today) {
        if (!event.isSharpDrop() && !event.fellBelowWeakThreshold()) return false;
        LocalDate horizon = today.plusDays(LOOKAHEAD_DAYS);
        boolean revisitedSoon = plan.getTasks().stream().filter(t -> !t.isDone())
                .filter(t -> event.conceptId.equals(t.getConceptId()))
                .map(StudyTask::getDate).anyMatch(d -> !d.isBefore(today) && !d.isAfter(horizon));
        return !revisitedSoon;
    }
}
