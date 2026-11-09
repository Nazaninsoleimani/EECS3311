package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.EventType;
import ca.yorku.eecs3311.studyagent.domain.HistoryFilter;
import ca.yorku.eecs3311.studyagent.domain.LearningEvent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * F16: everything the student did, newest first, filtered by course, period and activity type. Shown as a
 * timeline grouped by day, with colour-coded activities and a small summary of the period at the top.
 */
public class HistoryView extends BaseView {
    private static final String[] PERIODS = {"Today", "Last 7 days", "Last 30 days", "All time"};
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern RATING = Pattern.compile("^(AGAIN|HARD|GOOD|EASY):\\s*(.*)$", Pattern.DOTALL);
    private static final Pattern SCORE = Pattern.compile("^(Scored \\d+%)(.*)$", Pattern.DOTALL);
    private static final Pattern REMINDER_AT = Pattern.compile("^Reminder at (\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2})(?::\\d{2}(?:\\.\\d+)?)?: (.*)$", Pattern.DOTALL);

    private final JCheckBox courseOnly = new JCheckBox("Selected course only", true);
    private final JComboBox<String> period = new JComboBox<>(PERIODS);
    private final JComboBox<Object> type = new JComboBox<>();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"When", "Activity", "Details"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel count = new JLabel(" ");
    private final JPanel stats = new JPanel(new GridLayout(1, 5, 10, 0));
    private final CardLayout bodyLayout = new CardLayout();
    private final JPanel body = new JPanel(bodyLayout);
    private List<LearningEvent> shown = List.of();

    public HistoryView(GuiContext ctx) {
        super(ctx);
        period.setSelectedIndex(1);
        type.addItem("All activities");
        for (EventType t : EventType.values()) type.addItem(t);
        type.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean focus) {
                super.getListCellRendererComponent(l, v, i, sel, focus);
                if (v instanceof EventType t) setText(glyph(t) + "  " + label(t));
                return this;
            }
        });
        courseOnly.addActionListener(e -> render());
        period.addActionListener(e -> render());
        type.addActionListener(e -> render());

        JPanel filterRow = row(courseOnly, new JLabel("   Period"), period, new JLabel("   Activity"), type, count);
        filterRow.setOpaque(false);
        courseOnly.setOpaque(false);
        count.setForeground(Theme.muted());
        Card filters = new Card(new BorderLayout());
        filters.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        filters.add(filterRow, BorderLayout.CENTER);
        stats.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.add(heading("Learning history"), BorderLayout.NORTH);
        top.add(filters, BorderLayout.CENTER);
        top.add(stats, BorderLayout.SOUTH);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        configureTable();
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.border()));
        body.add(sp, "table");
        body.add(emptyState(), "empty");
        add(top, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
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
        shown = events;
        model.setRowCount(0);
        for (LearningEvent e : events) {
            model.addRow(new Object[]{e.getTimestamp(), e.getType(), e.getSummary()});
        }
        count.setText("   " + events.size() + " event(s)");
        updateStats(events);
        bodyLayout.show(body, events.isEmpty() ? "empty" : "table");
    }

    // ------------------------------------------------------------------ summary cards

    private void updateStats(List<LearningEvent> events) {
        stats.removeAll();
        stats.add(stat("Activities", events.size(), Theme.ACCENT));
        stats.add(stat("Quizzes taken", countOf(events, EventType.QUIZ_COMPLETED), new Color(0x2E9E5B)));
        stats.add(stat("Cards reviewed", countOf(events, EventType.FLASHCARD_REVIEWED), new Color(0x0D9488)));
        stats.add(stat("Tutor questions", countOf(events, EventType.QUESTION_ASKED), new Color(0x2563EB)));
        stats.add(stat("Tasks done", countOf(events, EventType.TASK_COMPLETED), new Color(0xEA580C)));
        stats.revalidate();
        stats.repaint();
    }

    private static long countOf(List<LearningEvent> events, EventType t) {
        return events.stream().filter(e -> e.getType() == t).count();
    }

    private static Card stat(String label, long value, Color tone) {
        Card c = new Card(new BorderLayout(0, 2));
        c.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel v = new JLabel(String.valueOf(value));
        v.setFont(v.getFont().deriveFont(Font.BOLD, 24f));
        v.setForeground(tone);
        JLabel l = new JLabel(label);
        l.setForeground(Theme.muted());
        c.add(v, BorderLayout.CENTER);
        c.add(l, BorderLayout.SOUTH);
        return c;
    }

    private JPanel emptyState() {
        Card empty = new Card(new BorderLayout(0, 6));
        empty.setBorder(BorderFactory.createEmptyBorder(40, 24, 40, 24));
        JLabel title = new JLabel("Nothing here yet", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel sub = new JLabel("<html><div style='text-align:center'>Try a longer period or \"All activities\". Everything you do "
                + "- uploads, quizzes, flashcards, tutor questions - appears here.</div></html>", SwingConstants.CENTER);
        sub.setForeground(Theme.muted());
        empty.add(title, BorderLayout.NORTH);
        empty.add(sub, BorderLayout.CENTER);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(empty, BorderLayout.NORTH);
        return wrap;
    }

    // ------------------------------------------------------------------ table

    private void configureTable() {
        table.setRowHeight(46);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(170);
        table.getColumnModel().getColumn(0).setMaxWidth(220);
        table.getColumnModel().getColumn(1).setPreferredWidth(230);
        table.getColumnModel().getColumn(1).setMaxWidth(300);
        table.getColumnModel().getColumn(2).setPreferredWidth(620);
        table.getColumnModel().getColumn(0).setCellRenderer(new WhenRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new ActivityRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new DetailsRenderer());
    }

    private LearningEvent at(int row) {
        return row >= 0 && row < shown.size() ? shown.get(row) : null;
    }

    /** True for the first (newest) event of each day: it carries the day heading and a separator line. */
    private boolean startsDay(int row) {
        LearningEvent e = at(row);
        LearningEvent prev = at(row - 1);
        return e != null && (prev == null || !prev.getTimestamp().toLocalDate().equals(e.getTimestamp().toLocalDate()));
    }

    /** Base renderer: draws a separator line above the first row of each day. */
    private class TimelineRenderer extends DefaultTableCellRenderer {
        protected boolean separator;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, false, row, col);
            separator = row > 0 && startsDay(row);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (separator) {
                g.setColor(Theme.border());
                g.fillRect(0, 0, getWidth(), 1);
            }
        }
    }

    private final class WhenRenderer extends TimelineRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            LocalDateTime when = (LocalDateTime) value;
            String time = when.format(TIME);
            if (startsDay(row)) {
                LocalDate d = when.toLocalDate();
                String day = d.equals(LocalDate.now()) ? "Today" : d.equals(LocalDate.now().minusDays(1)) ? "Yesterday" : DAY.format(d);
                setText("<html><b>" + day + "</b><br><font color='" + Theme.hex(sel ? getForeground() : Theme.muted()) + "'>" + time + "</font></html>");
            } else {
                setText("<html><font color='" + Theme.hex(sel ? getForeground() : Theme.muted()) + "'>" + time + "</font></html>");
            }
            return this;
        }
    }

    /** A coloured circle with an icon, then the activity name. */
    private final class ActivityRenderer extends TimelineRenderer {
        private Color tone = Theme.ACCENT;
        private String glyph = "";

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            EventType et = (EventType) value;
            tone = color(et);
            glyph = glyph(et);
            setText(label(et));
            setFont(t.getFont().deriveFont(Font.BOLD));
            setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 8));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int d = 28, x = 12, y = (getHeight() - d) / 2;
            g.setColor(Theme.tint(tone, 0.18));
            g.fillOval(x, y, d, d);
            g.setColor(tone);
            g.setStroke(new BasicStroke(1f));
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            int w = g.getFontMetrics().stringWidth(glyph);
            g.drawString(glyph, x + (d - w) / 2, y + d / 2 + g.getFontMetrics().getAscent() / 2 - 2);
            g.dispose();
        }
    }

    /** Details with the leading rating / score / reminder time shown as a coloured tag. */
    private final class DetailsRenderer extends TimelineRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            String s = String.valueOf(value);
            setToolTipText(s.length() > 80 ? "<html><div style='width:420px'>" + QuestionPanel.escape(s) + "</div></html>" : null);
            Matcher m;
            if ((m = RATING.matcher(s)).matches()) {
                Color c = switch (m.group(1)) {
                    case "AGAIN" -> Theme.DANGER;
                    case "HARD" -> Theme.WARNING.darker();
                    case "GOOD" -> Theme.SUCCESS;
                    default -> new Color(0x2563EB);
                };
                setText(html(tag(m.group(1).charAt(0) + m.group(1).substring(1).toLowerCase(), c, sel) + "&nbsp; " + QuestionPanel.escape(m.group(2))));
            } else if ((m = SCORE.matcher(s)).matches()) {
                Matcher pct = Pattern.compile("(\\d+)%").matcher(m.group(1));
                double v = pct.find() ? Integer.parseInt(pct.group(1)) / 100.0 : 0;
                setText(html(tag(m.group(1), Theme.forMastery(v), sel) + QuestionPanel.escape(m.group(2))));
            } else if ((m = REMINDER_AT.matcher(s)).matches()) {
                setText(html(tag(m.group(2) + (m.group(1).equals(LocalDate.now().toString()) ? "" : ", " + m.group(1)), Theme.muted(), sel)
                        + "&nbsp; " + QuestionPanel.escape(m.group(3))));
            } else {
                setText(html(QuestionPanel.escape(s)));
            }
            return this;
        }

        private String tag(String text, Color c, boolean sel) {
            return sel ? "<b>" + QuestionPanel.escape(text) + "</b>"
                    : "<span style='background-color:" + Theme.hex(Theme.tint(c, 0.16)) + "'><font color='" + Theme.hex(c) + "'><b>&nbsp;"
                    + QuestionPanel.escape(text) + "&nbsp;</b></font></span>";
        }

        private String html(String body) {
            return "<html>" + body + "</html>";
        }
    }

    // ------------------------------------------------------------------ activity presentation

    private static final Set<EventType> COURSE = Set.of(EventType.COURSE_CREATED, EventType.COURSE_UPDATED, EventType.COURSE_DELETED,
            EventType.MATERIAL_UPLOADED, EventType.MATERIAL_DELETED);
    private static final Set<EventType> AI = Set.of(EventType.MATERIAL_SUMMARIZED, EventType.CONCEPTS_EXTRACTED, EventType.QUESTION_ASKED,
            EventType.ANSWER_EXPLAINED, EventType.WEAK_TOPICS_ANALYZED);
    private static final Set<EventType> PLAN = Set.of(EventType.PLAN_GENERATED, EventType.PLAN_REVISED, EventType.TASK_COMPLETED);
    private static final Set<EventType> PRACTICE = Set.of(EventType.QUIZ_GENERATED, EventType.QUIZ_COMPLETED, EventType.SESSION_STARTED,
            EventType.SESSION_ENDED);
    private static final Set<EventType> CARDS = Set.of(EventType.FLASHCARDS_GENERATED, EventType.FLASHCARD_REVIEWED);

    static Color color(EventType t) {
        if (COURSE.contains(t)) return new Color(0x2563EB);
        if (AI.contains(t)) return Theme.ACCENT;
        if (PLAN.contains(t)) return new Color(0xEA580C);
        if (PRACTICE.contains(t)) return new Color(0x2E9E5B);
        if (CARDS.contains(t)) return new Color(0x0D9488);
        return new Color(0xDB2777);   // reminders
    }

    static String glyph(EventType t) {
        return switch (t) {
            case COURSE_CREATED, COURSE_UPDATED -> "▣";
            case COURSE_DELETED, MATERIAL_DELETED -> "✕";
            case MATERIAL_UPLOADED -> "↑";
            case MATERIAL_SUMMARIZED -> "≡";
            case CONCEPTS_EXTRACTED -> "✦";
            case QUESTION_ASKED, ANSWER_EXPLAINED -> "?";
            case WEAK_TOPICS_ANALYZED -> "◎";
            case PLAN_GENERATED, PLAN_REVISED -> "▦";
            case TASK_COMPLETED, QUIZ_COMPLETED -> "✓";
            case QUIZ_GENERATED -> "Q";
            case SESSION_STARTED, SESSION_ENDED -> "⚡";
            case FLASHCARDS_GENERATED, FLASHCARD_REVIEWED -> "▭";
            case REMINDER_CREATED, REMINDER_SENT -> "⏰";
        };
    }

    static String label(EventType t) {
        String s = t.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    JTable table() { return table; }
    JComboBox<Object> typeBox() { return type; }
}
