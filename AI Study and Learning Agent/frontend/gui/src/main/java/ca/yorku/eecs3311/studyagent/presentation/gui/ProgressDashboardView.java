package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.application.ProgressObserver;
import ca.yorku.eecs3311.studyagent.domain.ConceptMastery;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.LearningProgress;
import ca.yorku.eecs3311.studyagent.domain.ProgressEvent;
import ca.yorku.eecs3311.studyagent.domain.WeakTopic;
import ca.yorku.eecs3311.studyagent.domain.WeakTopicReport;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Comparator;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * F13 progress and F11 weak topics. As a ProgressObserver attached to the ProgressTracker it
 * refreshes itself whenever a graded activity changes a concept's mastery, wherever that happened.
 */
public class ProgressDashboardView extends BaseView implements ProgressObserver {
    private final JLabel summary = new JLabel(" ");
    private final JLabel live = new JLabel(" ");
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Concept", "Mastery", "Attempts", "Last practised", "State"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextArea weak = readOnlyArea();

    public ProgressDashboardView(GuiContext ctx) {
        super(ctx);
        table.getColumnModel().getColumn(1).setCellRenderer(new MasteryBar());
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading("Progress"), BorderLayout.NORTH);
        top.add(summary, BorderLayout.CENTER);
        top.add(live, BorderLayout.SOUTH);
        JPanel weakPanel = new JPanel(new BorderLayout());
        weakPanel.add(row(button("Analyze weak topics", this::loadWeakTopics)), BorderLayout.NORTH);
        weakPanel.add(scroll(weak), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll(table), weakPanel);
        split.setResizeWeight(0.55);
        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        facade.attachProgressObserver(this);
    }

    @Override public String title() { return "Progress"; }

    @Override
    public void render() {
        loadProgress();
    }

    public void loadProgress() {
        Course c = ctx.course();
        model.setRowCount(0);
        if (c == null) {
            summary.setText("Select a course first.");
            return;
        }
        LearningProgress p = facade.getProgress(ctx.studentId, c.getId());
        if (p.isEmpty()) {
            summary.setText("No graded activity yet: take a quiz, review flashcards or start a learning session.");
            return;
        }
        summary.setText(String.format("<html>Average mastery <b>%.0f%%</b> &nbsp; Quiz average <b>%.0f%%</b> &nbsp; Plan <b>%.0f%%</b> complete"
                        + " &nbsp; <b>%d</b> mastered &nbsp; <b>%d</b> graded activities</html>", p.averageMastery * 100, p.quizAverage * 100,
                p.planCompletion * 100, p.conceptsMastered, p.gradedActivities));
        List<ConceptMastery> sorted = p.masteries.stream().sorted(Comparator.comparingDouble(ConceptMastery::getMastery)).toList();
        for (ConceptMastery m : sorted) {
            model.addRow(new Object[]{m.getConceptName(), m.getMastery(), m.getAttempts(),
                    m.getLastPracticed() == null ? "" : m.getLastPracticed().toLocalDate(),
                    m.isWeak() ? "weak" : m.isMastered() ? "mastered" : m.getAttempts() < ConceptMastery.MIN_ATTEMPTS_FOR_WEAK ? "needs more practice" : ""});
        }
    }

    public void loadWeakTopics() {
        Course c = requireCourse();
        if (c == null) return;
        runAsync("Analyzing weak topics", () -> facade.analyzeWeakTopics(ctx.studentId, c.getId()), r -> weak.setText(describe(r)));
    }

    static String describe(WeakTopicReport r) {
        StringBuilder sb = new StringBuilder(r.message == null ? "" : r.message).append('\n');
        for (WeakTopic t : r.topics) {
            sb.append(String.format("%n• %s (mastery %.0f%%)%n   %s%n   Recommended next: %s%n", t.conceptName, t.mastery * 100,
                    t.diagnosis, t.recommendedActivity));
        }
        if (r.aiDiagnosed) sb.append("\nDiagnoses written by the AI from your wrong answers and the course material.");
        return sb.toString();
    }

    @Override
    public void onProgressChanged(ProgressEvent event) {
        SwingUtilities.invokeLater(() -> {
            live.setText(String.format("Updated: %s %.0f%% → %.0f%%", event.conceptName, event.oldMastery * 100, event.newMastery * 100));
            if (ctx.course() != null && ctx.course().getId().equals(event.courseId)) loadProgress();
        });
    }

    JTable table() { return table; }
    JLabel summaryLabel() { return summary; }
    JLabel liveLabel() { return live; }

    /** Draws the mastery column as a percentage bar. */
    private static final class MasteryBar extends JProgressBar implements TableCellRenderer {
        MasteryBar() {
            super(0, 100);
            setStringPainted(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            int v = (int) Math.round(((Double) value) * 100);
            setValue(v);
            setString(v + "%");
            return this;
        }
    }
}
