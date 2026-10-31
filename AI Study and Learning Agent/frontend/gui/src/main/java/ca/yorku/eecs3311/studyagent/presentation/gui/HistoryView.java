package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.EventType;
import ca.yorku.eecs3311.studyagent.domain.HistoryFilter;
import ca.yorku.eecs3311.studyagent.domain.LearningEvent;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** F16: everything the student did, newest first, filtered by course, period and activity type. */
public class HistoryView extends BaseView {
    private static final String[] PERIODS = {"Today", "Last 7 days", "Last 30 days", "All time"};
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JCheckBox courseOnly = new JCheckBox("Selected course only", true);
    private final JComboBox<String> period = new JComboBox<>(PERIODS);
    private final JComboBox<Object> type = new JComboBox<>();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"When", "Activity", "Details"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel count = new JLabel(" ");

    public HistoryView(GuiContext ctx) {
        super(ctx);
        period.setSelectedIndex(1);
        type.addItem("All activities");
        for (EventType t : EventType.values()) type.addItem(t);
        courseOnly.addActionListener(e -> render());
        period.addActionListener(e -> render());
        type.addActionListener(e -> render());
        table.getColumnModel().getColumn(2).setPreferredWidth(520);
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading("Learning history"), BorderLayout.NORTH);
        top.add(row(courseOnly, new JLabel("Period"), period, new JLabel("Activity"), type, count), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(scroll(table), BorderLayout.CENTER);
    }

    @Override public String title() { return "History"; }

    @Override
    public void render() {
        loadHistory(currentFilter());
    }

    HistoryFilter currentFilter() {
        Course c = ctx.course();
        String courseId = courseOnly.isSelected() && c != null ? c.getId() : null;
        LocalDate from = switch (period.getSelectedIndex()) {
            case 0 -> LocalDate.now();
            case 1 -> LocalDate.now().minusDays(7);
            case 2 -> LocalDate.now().minusDays(30);
            default -> null;
        };
        List<EventType> types = type.getSelectedItem() instanceof EventType t ? List.of(t) : List.of();
        return new HistoryFilter(courseId, from, null, types);
    }

    public void loadHistory(HistoryFilter filter) {
        List<LearningEvent> events = facade.getHistory(ctx.studentId, filter);
        model.setRowCount(0);
        for (LearningEvent e : events) {
            model.addRow(new Object[]{e.getTimestamp().format(WHEN), e.getType().name().toLowerCase().replace('_', ' '), e.getSummary()});
        }
        count.setText("   " + events.size() + " event(s)");
    }

    JTable table() { return table; }
    JComboBox<Object> typeBox() { return type; }
}
