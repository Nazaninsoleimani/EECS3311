package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.LearningProgress;
import ca.yorku.eecs3311.studyagent.domain.Reminder;
import ca.yorku.eecs3311.studyagent.domain.ReminderStatus;
import ca.yorku.eecs3311.studyagent.domain.StudyPlan;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import javax.swing.JTextArea;

/** Start page: exam countdown, today's plan (F06), progress (F13), due cards (F07) and next reminders (F14). */
public class DashboardView extends BaseView {
    private final JTextArea overview = readOnlyArea();

    public DashboardView(GuiContext ctx) {
        super(ctx);
        overview.setFont(overview.getFont().deriveFont(14f));
        add(heading("Overview"), BorderLayout.NORTH);
        add(scroll(overview), BorderLayout.CENTER);
        add(row(button("Refresh", this::render)), BorderLayout.SOUTH);
    }

    @Override public String title() { return "Dashboard"; }

    @Override
    public void render() {
        loadOverview();
    }

    public void loadOverview() {
        overview.setText(buildOverview(LocalDate.now()));
        overview.setCaretPosition(0);
    }

    String buildOverview(LocalDate today) {
        StringBuilder sb = new StringBuilder("Hello, ").append(facade.getStudent(ctx.studentId).getName()).append(".\n");
        sb.append(facade.isLlmAvailable() ? "" : "\nAI is not configured: copy .env.example to .env and add an API key (see README)."
                + " Retrieval, grading, scheduling and progress still work.\n");
        Course c = ctx.course();
        if (c == null) {
            return sb.append("\nStart by creating a course in the Courses tab, then upload your lecture notes in Materials.").toString();
        }
        sb.append("\n").append(c.getCode()).append(" - ").append(c.getTitle());
        if (c.getExamDate() != null) {
            int days = c.daysUntilExam(today);
            sb.append(days >= 0 ? "   |   exam in " + days + " day(s) (" + c.getExamDate() + ")" : "   |   exam date has passed");
        }
        sb.append("\n\nTODAY'S PLAN\n");
        StudyPlan plan = facade.getActivePlan(ctx.studentId, c.getId());
        if (plan == null) {
            sb.append("  No study plan yet (Study Plan tab).\n");
        } else {
            List<StudyTask> todays = plan.tasksFor(today);
            if (todays.isEmpty()) sb.append("  Nothing scheduled today.\n");
            todays.forEach(t -> sb.append(String.format("  [%s] %d min  %s%n", t.isDone() ? "x" : " ", t.getDurationMinutes(), t.getObjective())));
            sb.append(String.format("  Plan %.0f%% complete (%s).%n", plan.completionRate() * 100, plan.getStrategyName()));
        }
        sb.append("\nPROGRESS\n");
        LearningProgress p = facade.getProgress(ctx.studentId, c.getId());
        sb.append(p.isEmpty() ? "  No graded practice yet.\n" : String.format("  Average mastery %.0f%%, quiz average %.0f%%, %d concept(s) mastered.%n",
                p.averageMastery * 100, p.quizAverage * 100, p.conceptsMastered));
        long weak = p.masteries.stream().filter(m -> m.isWeak()).count();
        if (weak > 0) sb.append("  ").append(weak).append(" weak concept(s): see the Progress tab.\n");
        sb.append("\nFLASHCARDS\n  ").append(facade.dueFlashcards(c.getId()).size()).append(" card(s) due for review.\n");
        sb.append("\nUPCOMING REMINDERS\n");
        List<Reminder> next = facade.listReminders(ctx.studentId).stream().filter(r -> r.getStatus() == ReminderStatus.PENDING)
                .sorted(Comparator.comparing(Reminder::getDueAt)).limit(3).toList();
        if (next.isEmpty()) sb.append("  None.\n");
        next.forEach(r -> sb.append("  ").append(r.getDueAt().format(ReminderView.WHEN)).append("  ").append(r.getMessage()).append('\n'));
        return sb.toString();
    }

    JTextArea overviewArea() { return overview; }
}
