package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.ActivityType;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.StudyPlan;
import ca.yorku.eecs3311.studyagent.domain.StudyPlanRequest;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * F06: generate the AI study plan, follow it task by task, and see automatic revisions. An overview card shows the
 * strategy, completion, days to the exam and today's tasks; the table colours activities, groups days and lets the
 * student tick tasks off or jump straight into one.
 */
public class StudyPlannerView extends BaseView {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);

    private final JTextField exam = new JTextField(10);
    private final JSpinner minutes = new JSpinner(new SpinnerNumberModel(60, 15, 480, 15));
    private final JTextField focus = new JTextField(22);
    private final JTextArea rationale = QuestionPanel.wrapping("", 13.5f, Font.PLAIN);
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Done", "Date", "Min", "Activity", "Concept", "Objective"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : Object.class; }
    };
    private final JTable table = new JTable(model);
    private List<StudyTask> shown = List.of();

    // Overview card.
    private final JLabel percent = new JLabel("0%");
    private final Card.Pill strategy = new Card.Pill("", Theme.ACCENT);
    private final Card.Pill tasksPill = new Card.Pill("", Theme.muted());
    private final Card.Pill examPill = new Card.Pill("", Theme.muted());
    private final Card.Pill revisionPill = new Card.Pill("", Theme.muted());
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JLabel today = new JLabel(" ");
    private final JLabel warnings = new JLabel(" ");
    private final JPanel overview = new JPanel(new BorderLayout());

    public StudyPlannerView(GuiContext ctx) {
        super(ctx);
        focus.putClientProperty("JTextField.placeholderText", "optional, e.g. Inheritance, Generics");
        exam.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");

        JPanel settingsRow = row(new JLabel("Exam date"), exam, new JLabel("   Minutes per day"), minutes,
                new JLabel("   Focus topics"), focus, accentButton("Generate plan", this::submitPlanRequest));
        settingsRow.setOpaque(false);
        Card settings = new Card(new BorderLayout());
        settings.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        settings.add(settingsRow, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.add(heading("Study plan"), BorderLayout.NORTH);
        top.add(settings, BorderLayout.CENTER);
        overview.setOpaque(false);
        top.add(overview, BorderLayout.SOUTH);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        configureTable();
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(Theme.border()));

        JPanel bottom = row(accentButton("Start selected task →", this::startSelected), button("Mark selected task done", this::markSelectedDone),
                hint("  Tip: tick the box to complete a task, or double-click a task to start it."));
        add(top, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        facade.setPlanRevisionListener(p -> SwingUtilities.invokeLater(() -> {
            render();
            ctx.status("Your study plan was revised automatically after a drop in mastery.");
        }));
    }

    @Override public String title() { return "Study Plan"; }

    @Override
    public void render() {
        Course c = ctx.course();
        if (c == null) {
            displayPlan(null);
            return;
        }
        exam.setText(c.getExamDate() == null ? "" : c.getExamDate().toString());
        displayPlan(facade.getActivePlan(ctx.studentId, c.getId()));
    }

    public void submitPlanRequest() {
        Course c = requireCourse();
        if (c == null) return;
        StudyPlanRequest request;
        try {
            List<String> topics = Arrays.stream(focus.getText().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            request = new StudyPlanRequest(ctx.studentId, c.getId(), examDate(), (Integer) minutes.getValue(), topics);
        } catch (ValidationException e) {
            showError(e.getMessage());
            return;
        }
        runAsync("Planning (agent: analyze, retrieve, prioritize, schedule)", () -> facade.generateStudyPlan(request), p -> {
            displayPlan(p);
            ctx.fireDataChanged();
        });
    }

    public void displayPlan(StudyPlan plan) {
        model.setRowCount(0);
        shown = plan == null ? List.of() : plan.getTasks();
        overview.removeAll();
        if (plan == null) {
            rationale.setText(ctx.course() == null ? "Select a course first." : "No active plan yet. Choose your daily time and press Generate plan.");
            overview.add(emptyCard(), BorderLayout.CENTER);
        } else {
            rationale.setText(plan.getRationale());
            overview.add(overviewCard(plan), BorderLayout.CENTER);
            for (StudyTask t : shown) {
                model.addRow(new Object[]{t.isDone(), t.getDate(), t.getDurationMinutes(), t.getActivity(), t.getConceptName(), t.getObjective()});
            }
        }
        overview.revalidate();
        overview.repaint();
    }

    private Card emptyCard() {
        Card c = new Card(new BorderLayout(0, 6));
        c.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        JLabel title = new JLabel(ctx.course() == null ? "Select a course first" : "Let the agent plan your studying", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        c.add(title, BorderLayout.NORTH);
        if (ctx.course() != null) {
            JLabel sub = new JLabel("<html><div style='text-align:center'>Set your exam date and how many minutes you can study each day, "
                    + "then press <b>Generate plan</b>.<br>Weak topics come first, and the plan revises itself when your mastery drops.</div></html>",
                    SwingConstants.CENTER);
            sub.setForeground(Theme.muted());
            c.add(sub, BorderLayout.CENTER);
        }
        return c;
    }

    private Card overviewCard(StudyPlan plan) {
        double done = plan.completionRate();
        long doneCount = shown.stream().filter(StudyTask::isDone).count();
        LocalDate now = LocalDate.now();

        Card card = new Card(new BorderLayout(20, 0), Theme.ACCENT);
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        percent.setText(Math.round(done * 100) + "%");
        percent.setFont(percent.getFont().deriveFont(Font.BOLD, 38f));
        percent.setForeground(Theme.ACCENT);
        JLabel complete = new JLabel("complete", SwingConstants.CENTER);
        complete.setForeground(Theme.muted());
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(percent, BorderLayout.CENTER);
        left.add(complete, BorderLayout.SOUTH);
        card.add(left, BorderLayout.WEST);

        strategy.set(plan.getStrategyName().toUpperCase(Locale.ROOT) + " STRATEGY", Theme.ACCENT);
        tasksPill.set(doneCount + " of " + shown.size() + " tasks done", Theme.muted());
        LocalDate examDate = ctx.course() == null ? null : ctx.course().getExamDate();
        long days = examDate == null ? -1 : ChronoUnit.DAYS.between(now, examDate);
        examPill.set(examDate == null ? "No exam date" : days > 1 ? days + " days to the exam" : days == 1 ? "Exam tomorrow"
                : days == 0 ? "Exam today" : "Exam passed", days >= 0 && days <= 3 ? Theme.DANGER : Theme.muted());
        revisionPill.set(plan.getRevision() == 0 ? "Original plan" : "Revised " + plan.getRevision() + "x", Theme.muted());
        JPanel pills = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pills.setOpaque(false);
        for (JLabel p : List.of(strategy, examPill, tasksPill, revisionPill)) pills.add(p);

        progress.setValue((int) Math.round(done * 100));
        progress.setPreferredSize(new Dimension(10, 8));

        List<StudyTask> todays = plan.tasksFor(now);
        int todayMinutes = todays.stream().mapToInt(StudyTask::getDurationMinutes).sum();
        long todayLeft = todays.stream().filter(t -> !t.isDone()).count();
        String todayList = todays.stream().map(t -> (t.isDone() ? "✓ " : "") + QuestionPanel.escape(t.getConceptName())
                + " <font color='" + Theme.hex(Theme.muted()) + "'>(" + activityLabel(t.getActivity()) + ")</font>").reduce((a, b) -> a + " &nbsp;&middot;&nbsp; " + b).orElse("");
        today.setText(todays.isEmpty() ? "<html><b>Today:</b> nothing scheduled - a good day to review.</html>"
                : "<html><b>Today</b> &nbsp;" + todayMinutes + " min, " + (todayLeft == 0 ? "all done!" : todayLeft + " left") + " &nbsp;&mdash;&nbsp; " + todayList + "</html>");

        warnings.setText(plan.getWarnings().isEmpty() ? "" : "<html>⚠ " + QuestionPanel.escape(String.join(" · ", plan.getWarnings())) + "</html>");
        warnings.setForeground(Theme.WARNING.darker());

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(pills, BorderLayout.NORTH);
        body.add(rationale, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setOpaque(false);
        south.add(progress, BorderLayout.NORTH);
        south.add(today, BorderLayout.CENTER);
        if (!plan.getWarnings().isEmpty()) south.add(warnings, BorderLayout.SOUTH);
        body.add(south, BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------ table

    private void configureTable() {
        table.setRowHeight(40);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setMaxWidth(56);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setMaxWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        table.getColumnModel().getColumn(4).setPreferredWidth(200);
        table.getColumnModel().getColumn(5).setPreferredWidth(420);
        PlanCellRenderer r = new PlanCellRenderer();
        for (int i = 1; i < 6; i++) table.getColumnModel().getColumn(i).setCellRenderer(i == 3 ? new ActivityRenderer() : r);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0 || row >= shown.size()) return;
                if (table.columnAtPoint(e.getPoint()) == 0 && !shown.get(row).isDone()) markTaskComplete(shown.get(row).getId());
                else if (e.getClickCount() == 2) start(shown.get(row));
            }
        });
    }

    /** Background for a row: today highlighted, days alternately shaded so each day reads as a group. */
    private Color rowBackground(int row) {
        StudyTask t = shown.get(row);
        if (t.getDate().equals(LocalDate.now())) return Theme.tint(Theme.ACCENT, 0.08);
        long dayIndex = shown.stream().map(StudyTask::getDate).distinct().filter(d -> d.isBefore(t.getDate())).count();
        return dayIndex % 2 == 0 ? Theme.card() : Theme.tint(Theme.text(), 0.035);
    }

    private final class PlanCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, false, row, col);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setToolTipText(null);
            if (row >= shown.size()) return this;
            StudyTask task = shown.get(row);
            LocalDate now = LocalDate.now();
            Font base = t.getFont();
            setFont(base);
            setForeground(task.isDone() ? Theme.muted() : Theme.text());
            if (!sel) setBackground(rowBackground(row));
            switch (col) {
                case 1 -> {
                    LocalDate d = task.getDate();
                    boolean overdue = d.isBefore(now) && !task.isDone();
                    setText(d.equals(now) ? "Today" : d.equals(now.plusDays(1)) ? "Tomorrow" : DAY.format(d));
                    if (d.equals(now)) {
                        setFont(base.deriveFont(Font.BOLD));
                        if (!sel) setForeground(Theme.ACCENT);
                    }
                    if (overdue) {
                        setText(DAY.format(d) + "  · overdue");
                        if (!sel) setForeground(Theme.DANGER);
                    }
                }
                case 2 -> setText(value + " min");
                case 4 -> {
                    setFont(base.deriveFont(Font.BOLD));
                    setText((task.isDone() ? "✓ " : "") + value);
                }
                case 5 -> setToolTipText("<html><div style='width:360px'>" + QuestionPanel.escape(String.valueOf(value)) + "</div></html>");
                default -> { }
            }
            return this;
        }
    }

    /** The activity as a rounded, colour-coded tag drawn inside the cell. */
    private final class ActivityRenderer extends DefaultTableCellRenderer {
        private Color tone = Theme.ACCENT;
        private Color rowBg = Theme.card();

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, false, row, col);
            ActivityType a = (ActivityType) value;
            boolean done = row < shown.size() && shown.get(row).isDone();
            tone = done ? Theme.muted() : activityColor(a);
            rowBg = sel ? t.getSelectionBackground() : row < shown.size() ? rowBackground(row) : Theme.card();
            setText(activityLabel(a));
            setFont(t.getFont().deriveFont(Font.BOLD, 12f));
            setForeground(tone);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 10));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(rowBg);
            g.fillRect(0, 0, getWidth(), getHeight());
            int textW = g.getFontMetrics(getFont()).stringWidth(getText());
            int h = 24, y = (getHeight() - h) / 2;
            g.setColor(Theme.tint(tone, 0.16));
            g.fillRoundRect(10, y, textW + 20, h, h, h);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    static String activityLabel(ActivityType a) {
        return switch (a) {
            case READ -> "Read";
            case FLASHCARDS -> "Flashcards";
            case QUIZ -> "Quiz";
            case ADAPTIVE_SESSION -> "Learning session";
            case REVIEW -> "Review";
        };
    }

    static Color activityColor(ActivityType a) {
        return switch (a) {
            case READ -> new Color(0x2563EB);
            case FLASHCARDS -> new Color(0x0D9488);
            case QUIZ -> Theme.ACCENT;
            case ADAPTIVE_SESSION -> new Color(0xEA580C);
            case REVIEW -> new Color(0xDB2777);
        };
    }

    // ------------------------------------------------------------------ actions

    /** Opens the tab that does this task: a quiz on its concept, the flashcards, the materials ... */
    private void start(StudyTask t) {
        switch (t.getActivity()) {
            case QUIZ -> ctx.requestQuizOn(t.getConceptName());
            case FLASHCARDS -> ctx.navigateTo(FlashcardView.class);
            case READ -> ctx.navigateTo(MaterialView.class);
            case ADAPTIVE_SESSION -> ctx.navigateTo(LearningSessionView.class);
            case REVIEW -> ctx.navigateTo(HistoryView.class);
        }
    }

    private void startSelected() {
        StudyTask t = selectedTask();
        if (t != null) start(t);
    }

    public void markTaskComplete(String taskId) {
        attempt(() -> {
            facade.completeStudyTask(taskId);
            render();
            ctx.fireDataChanged();
        });
    }

    private void markSelectedDone() {
        StudyTask t = selectedTask();
        if (t != null) markTaskComplete(t.getId());
    }

    private StudyTask selectedTask() {
        int i = table.getSelectedRow();
        if (i < 0 || i >= shown.size()) {
            showError("Select a task in the table first.");
            return null;
        }
        return shown.get(i);
    }

    private LocalDate examDate() {
        String s = exam.getText().trim();
        if (s.isEmpty()) return null;
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Exam date must look like 2026-12-15");
        }
    }

    JTable table() { return table; }
    JTextArea rationaleArea() { return rationale; }
    JLabel strategyLabel() { return strategy; }
}
