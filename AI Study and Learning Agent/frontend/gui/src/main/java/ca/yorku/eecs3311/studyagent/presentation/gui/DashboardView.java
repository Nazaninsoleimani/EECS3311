package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.ConceptMastery;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.LearningProgress;
import ca.yorku.eecs3311.studyagent.domain.Reminder;
import ca.yorku.eecs3311.studyagent.domain.ReminderStatus;
import ca.yorku.eecs3311.studyagent.domain.StudyPlan;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

/**
 * Start page: exam countdown, plan progress, mastery and due cards as stat cards; today's tasks, the
 * weakest concepts and upcoming reminders; quick actions into the other views. Overview of F06, F07,
 * F11, F13 and F14.
 */
public class DashboardView extends BaseView {
    /** Everything the dashboard shows, computed from the facade in one place. */
    record Overview(String studentName, Course course, Integer daysToExam, StudyPlan plan, List<StudyTask> today,
                    LearningProgress progress, List<ConceptMastery> weakest, int dueCards, List<Reminder> reminders,
                    boolean aiAvailable) {}

    private final JPanel content = new JPanel();

    public DashboardView(GuiContext ctx) {
        super(ctx);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        add(sp, BorderLayout.CENTER);
    }

    @Override public String title() { return "Dashboard"; }

    @Override
    public void render() {
        loadOverview();
    }

    public void loadOverview() {
        Overview o = overview(LocalDate.now());
        content.removeAll();
        content.add(greeting(o));
        if (!o.aiAvailable()) content.add(spaced(aiBanner()));
        if (o.course() == null) {
            content.add(spaced(emptyState()));
        } else {
            content.add(spaced(statRow(o)));
            JPanel middle = new JPanel(new GridLayout(1, 2, 14, 0));
            middle.setOpaque(false);
            middle.add(Card.titled("Today's plan", todayList(o)));
            middle.add(Card.titled("Weakest concepts", weakestList(o)));
            content.add(spaced(middle));
            JPanel bottom = new JPanel(new GridLayout(1, 2, 14, 0));
            bottom.setOpaque(false);
            bottom.add(Card.titled("Quick actions", quickActions()));
            bottom.add(Card.titled("Upcoming reminders", remindersList(o)));
            content.add(spaced(bottom));
        }
        content.add(Box.createVerticalGlue());
        content.revalidate();
        content.repaint();
    }

    Overview overview(LocalDate today) {
        String name = facade.getStudent(ctx.studentId).getName();
        Course c = ctx.course();
        if (c == null) return new Overview(name, null, null, null, List.of(), null, List.of(), 0, List.of(), facade.isLlmAvailable());
        StudyPlan plan = facade.getActivePlan(ctx.studentId, c.getId());
        LearningProgress p = facade.getProgress(ctx.studentId, c.getId());
        List<ConceptMastery> weakest = p.masteries.stream().filter(m -> m.getAttempts() > 0)
                .sorted(Comparator.comparingDouble(ConceptMastery::getMastery)).limit(4).toList();
        List<Reminder> next = facade.listReminders(ctx.studentId).stream().filter(r -> r.getStatus() == ReminderStatus.PENDING)
                .sorted(Comparator.comparing(Reminder::getDueAt)).limit(3).toList();
        Integer days = c.getExamDate() == null ? null : c.daysUntilExam(today);
        return new Overview(name, c, days, plan, plan == null ? List.of() : plan.tasksFor(today), p, weakest,
                facade.dueFlashcards(c.getId()).size(), next, facade.isLlmAvailable());
    }

    // ------------------------------------------------------------------ pieces

    private JComponent greeting(Overview o) {
        int hour = LocalTime.now().getHour();
        String hello = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        JLabel h = heading(hello + ", " + o.studentName());
        h.setFont(h.getFont().deriveFont(Font.BOLD, 24f));
        p.add(h);
        p.add(hint(o.course() == null ? "Let's set up your first course." : o.course().getCode() + "  ·  " + o.course().getTitle()));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JComponent aiBanner() {
        Card c = new Card(new BorderLayout(12, 0), Theme.WARNING);
        JLabel text = new JLabel("<html><b>AI features are off.</b> Summaries, quizzes, flashcards, study plans and tutor answers "
                + "need an OpenAI API key. Everything else already works.</html>");
        c.add(text, BorderLayout.CENTER);
        c.add(button("How to switch on", () -> StudyAgentGui.showAiHelp(this)), BorderLayout.EAST);
        return c;
    }

    private JComponent emptyState() {
        Card c = new Card(new BorderLayout(0, 10));
        JLabel l = new JLabel("<html><span style='font-size:13px'><b>Start in three steps</b></span><br><br>"
                + "1. Create a course with its exam date.<br>2. Upload your lecture notes (PDF, Word, text or Markdown).<br>"
                + "3. Ask the tutor, generate a quiz or build a study plan.</html>");
        c.add(l, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.add(accentButton("Create a course", () -> ctx.navigateTo(CourseView.class)));
        c.add(actions, BorderLayout.SOUTH);
        return c;
    }

    private JComponent statRow(Overview o) {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);
        String exam = o.daysToExam() == null ? "-" : o.daysToExam() < 0 ? "Done" : String.valueOf(o.daysToExam());
        String examSub = o.daysToExam() == null ? "no exam date set" : o.daysToExam() == 1 ? "day until the exam" : "days until the exam";
        row.add(stat("Exam", exam, examSub, o.daysToExam() != null && o.daysToExam() <= 7 ? Theme.DANGER : Theme.ACCENT));
        String plan = o.plan() == null ? "-" : Math.round(o.plan().completionRate() * 100) + "%";
        row.add(stat("Study plan", plan, o.plan() == null ? "no plan yet" : o.plan().getStrategyName(), Theme.ACCENT));
        boolean practised = o.progress() != null && !o.progress().isEmpty();
        row.add(stat("Mastery", practised ? Math.round(o.progress().averageMastery * 100) + "%" : "-",
                practised ? o.progress().conceptsMastered + " concept(s) mastered" : "no graded practice yet",
                practised ? Theme.forMastery(o.progress().averageMastery) : Theme.ACCENT));
        row.add(stat("Flashcards", String.valueOf(o.dueCards()), "cards due today", o.dueCards() > 0 ? Theme.WARNING : Theme.SUCCESS));
        return row;
    }

    private static Card stat(String label, String value, String sub, Color color) {
        Card c = new Card(null);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label.toUpperCase());
        l.setForeground(Theme.muted());
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11f));
        JLabel v = new JLabel(value);
        v.setFont(v.getFont().deriveFont(Font.BOLD, 30f));
        v.setForeground(color);
        JLabel s = new JLabel(sub);
        s.setForeground(Theme.muted());
        c.add(l);
        c.add(Box.createVerticalStrut(4));
        c.add(v);
        c.add(s);
        Animations.countUp(v, 700);
        return c;
    }

    private JComponent todayList(Overview o) {
        JPanel list = column();
        if (o.plan() == null) {
            list.add(hint("No study plan yet."));
            list.add(Box.createVerticalStrut(8));
            list.add(left(accentButton("Generate a plan", () -> ctx.navigateTo(StudyPlannerView.class))));
            return list;
        }
        if (o.today().isEmpty()) list.add(hint("Nothing scheduled today. Enjoy the break or review flashcards."));
        for (StudyTask t : o.today()) {
            JLabel l = new JLabel((t.isDone() ? "✓  " : "○  ") + t.getDurationMinutes() + " min  ·  " + t.getObjective());
            if (t.isDone()) l.setForeground(Theme.muted());
            list.add(l);
            list.add(Box.createVerticalStrut(6));
        }
        return list;
    }

    private JComponent weakestList(Overview o) {
        JPanel list = column();
        if (o.weakest().isEmpty()) {
            list.add(hint("Take a quiz or an adaptive session to see where you stand."));
            list.add(Box.createVerticalStrut(8));
            list.add(left(button("Start adaptive practice", () -> ctx.navigateTo(LearningSessionView.class))));
            return list;
        }
        for (ConceptMastery m : o.weakest()) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            JLabel name = new JLabel(m.getConceptName());
            name.setPreferredSize(new Dimension(170, 22));
            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue((int) Math.round(m.getMastery() * 100));
            bar.setStringPainted(true);
            bar.setString(bar.getValue() + "%");
            bar.setForeground(Theme.forMastery(m.getMastery()));
            Animations.fill(bar, 800);
            row.add(name, BorderLayout.WEST);
            row.add(bar, BorderLayout.CENTER);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            list.add(row);
            list.add(Box.createVerticalStrut(8));
        }
        return list;
    }

    private JComponent quickActions() {
        JPanel grid = new JPanel(new GridLayout(2, 3, 8, 8));
        grid.setOpaque(false);
        grid.add(accentButton("Ask the tutor", () -> ctx.navigateTo(TutorChatView.class)));
        grid.add(button("Take a quiz", () -> ctx.navigateTo(QuizView.class)));
        grid.add(button("Adaptive practice", () -> ctx.navigateTo(LearningSessionView.class)));
        grid.add(button("Review flashcards", () -> ctx.navigateTo(FlashcardView.class)));
        grid.add(button("Upload notes", () -> ctx.navigateTo(MaterialView.class)));
        grid.add(button("Concept map", () -> ctx.navigateTo(ConceptMapView.class)));
        return grid;
    }

    private JComponent remindersList(Overview o) {
        JPanel list = column();
        if (o.reminders().isEmpty()) list.add(hint("No reminders scheduled."));
        for (Reminder r : o.reminders()) {
            JLabel l = new JLabel("<html><b>" + r.getDueAt().format(ReminderView.WHEN) + "</b>  " + QuestionPanel.escape(r.getMessage()) + "</html>");
            list.add(l);
            list.add(Box.createVerticalStrut(6));
        }
        return list;
    }

    private static JPanel column() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    private static JComponent left(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.add(c);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private static JComponent spaced(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        p.add(c, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    JPanel contentPanel() { return content; }
}
