package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.StudyPlan;
import ca.yorku.eecs3311.studyagent.domain.StudyPlanRequest;
import ca.yorku.eecs3311.studyagent.domain.StudyTask;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** F06: generate the AI study plan, follow it task by task, and see automatic revisions. */
public class StudyPlannerView extends BaseView {
    private final JTextField exam = new JTextField(10);
    private final JSpinner minutes = new JSpinner(new SpinnerNumberModel(60, 15, 480, 15));
    private final JTextField focus = new JTextField(22);
    private final JTextArea rationale = readOnlyArea();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Done", "Date", "Min", "Activity", "Concept", "Objective"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : Object.class; }
    };
    private final JTable table = new JTable(model);
    private List<StudyTask> shown = List.of();

    public StudyPlannerView(GuiContext ctx) {
        super(ctx);
        rationale.setRows(4);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(2).setMaxWidth(50);
        table.getColumnModel().getColumn(5).setPreferredWidth(380);
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading("Study plan"), BorderLayout.NORTH);
        top.add(row(new JLabel("Exam date"), exam, new JLabel("Minutes per day"), minutes, new JLabel("Focus topics (comma-separated)"),
                focus, button("Generate plan", this::submitPlanRequest)), BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll(rationale), scroll(table));
        split.setResizeWeight(0.2);
        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(row(button("Mark selected task done", this::markSelectedDone)), BorderLayout.SOUTH);
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
        if (plan == null) {
            rationale.setText(ctx.course() == null ? "Select a course first." : "No active plan yet. Choose your daily time and press Generate plan.");
            return;
        }
        StringBuilder sb = new StringBuilder(String.format("%s strategy, revision %d, %.0f%% complete.%n%s", plan.getStrategyName(),
                plan.getRevision(), plan.completionRate() * 100, plan.getRationale()));
        plan.getWarnings().forEach(w -> sb.append("\nNote: ").append(w));
        rationale.setText(sb.toString());
        LocalDate today = LocalDate.now();
        for (StudyTask t : shown) {
            model.addRow(new Object[]{t.isDone(), t.getDate().equals(today) ? t.getDate() + " (today)" : t.getDate(),
                    t.getDurationMinutes(), t.getActivity(), t.getConceptName(), t.getObjective()});
        }
    }

    public void markTaskComplete(String taskId) {
        attempt(() -> {
            facade.completeStudyTask(taskId);
            render();
            ctx.fireDataChanged();
        });
    }

    private void markSelectedDone() {
        int i = table.getSelectedRow();
        if (i < 0 || i >= shown.size()) {
            showError("Select a task in the table first.");
            return;
        }
        markTaskComplete(shown.get(i).getId());
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
}
