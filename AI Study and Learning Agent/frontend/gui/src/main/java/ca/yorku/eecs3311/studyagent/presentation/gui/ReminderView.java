package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.Recurrence;
import ca.yorku.eecs3311.studyagent.domain.Reminder;
import ca.yorku.eecs3311.studyagent.domain.ReminderStatus;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** F14: schedule, list and cancel study reminders; due ones pop up as desktop notifications. */
public class ReminderView extends BaseView {
    static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final JTextField when = new JTextField(14);
    private final JComboBox<Recurrence> repeat = new JComboBox<>(Recurrence.values());
    private final JTextField message = new JTextField(30);
    /** How long before the reminder time an extra early reminder goes off (minutes; 0 = none). */
    private static final int[] LEAD_MINUTES = {0, 5, 15, 30, 60, 24 * 60};
    private final JComboBox<String> before = new JComboBox<>(new String[]{"No early reminder", "5 minutes before",
            "15 minutes before", "30 minutes before", "1 hour before", "1 day before"});
    private final JCheckBox showAll = new JCheckBox("Show sent and cancelled");
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"When", "Repeats", "Status", "Message"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel summary = new JLabel(" ");
    private final CardLayout listLayout = new CardLayout();
    private final JPanel list = new JPanel(listLayout);
    private final JLabel emptyTitle = new JLabel("", SwingConstants.CENTER);
    private List<Reminder> shown = List.of();

    public ReminderView(GuiContext ctx) {
        super(ctx);
        message.putClientProperty("JTextField.placeholderText", "e.g. Review the observer pattern flashcards");
        when.putClientProperty("JTextField.placeholderText", "yyyy-mm-dd hh:mm");
        repeat.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean focus) {
                super.getListCellRendererComponent(l, v, i, sel, focus);
                setText(repeatLabel((Recurrence) v));
                return this;
            }
        });
        showAll.addActionListener(e -> render());

        add(header(), BorderLayout.NORTH);
        add(listArea(), BorderLayout.CENTER);
        JPanel bottom = row(button("Cancel selected", this::cancelSelected), button("Send due reminders now", this::dispatch), showAll);
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel header() {
        JLabel newTitle = new JLabel("New reminder");
        newTitle.setFont(newTitle.getFont().deriveFont(Font.BOLD, 14f));
        JPanel fields = row(new JLabel("When"), when, new JLabel("   Repeat"), repeat, new JLabel("   Also remind me"), before);
        JPanel messageRow = row(new JLabel("Message"), message, accentButton("Add reminder", this::createReminder),
                new JLabel("   "), button("Remind me before the exam", this::createExamReminders));
        JPanel quick = row(hint("Quick pick:"), quick("In 1 hour", () -> LocalDateTime.now().plusHours(1)),
                quick("Tonight 20:00", () -> LocalDate.now().atTime(20, 0)),
                quick("Tomorrow 09:00", () -> LocalDate.now().plusDays(1).atTime(9, 0)),
                quick("Tomorrow 18:00", () -> LocalDate.now().plusDays(1).atTime(18, 0)));
        for (JPanel p : List.of(fields, messageRow, quick)) p.setOpaque(false);
        JPanel rows = new JPanel(new BorderLayout(0, 4));
        rows.setOpaque(false);
        rows.add(fields, BorderLayout.NORTH);
        rows.add(messageRow, BorderLayout.CENTER);
        rows.add(quick, BorderLayout.SOUTH);
        JPanel form = new JPanel(new BorderLayout(0, 6));
        form.setOpaque(false);
        form.add(newTitle, BorderLayout.NORTH);
        form.add(rows, BorderLayout.CENTER);
        Card card = new Card(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));
        card.add(form, BorderLayout.CENTER);

        summary.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.add(heading("Reminders"), BorderLayout.NORTH);
        top.add(card, BorderLayout.CENTER);
        top.add(summary, BorderLayout.SOUTH);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return top;
    }

    private JButton quick(String label, java.util.function.Supplier<LocalDateTime> time) {
        JButton b = button(label, () -> {
            LocalDateTime t = time.get().withSecond(0).withNano(0);
            when.setText(t.format(WHEN));
            message.requestFocusInWindow();
        });
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setFont(b.getFont().deriveFont(12f));
        return b;
    }

    private JPanel listArea() {
        table.setRowHeight(52);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(230);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(480);
        table.getColumnModel().getColumn(0).setCellRenderer(new WhenRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new TagRenderer(true));
        table.getColumnModel().getColumn(2).setCellRenderer(new TagRenderer(false));
        table.getColumnModel().getColumn(3).setCellRenderer(new MessageRenderer());
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.border()));

        Card empty = new Card(new BorderLayout(0, 6));
        empty.setBorder(BorderFactory.createEmptyBorder(40, 24, 40, 24));
        JLabel bell = new JLabel("🔔", SwingConstants.CENTER);
        bell.setFont(bell.getFont().deriveFont(30f));
        emptyTitle.setFont(emptyTitle.getFont().deriveFont(Font.BOLD, 18f));
        JLabel sub = new JLabel("<html><div style='text-align:center'>Pick a time above (or a quick pick), write what to study, and press "
                + "<b>Add reminder</b>.<br>Your study plan also adds a reminder for each planned day.</div></html>", SwingConstants.CENTER);
        sub.setForeground(Theme.muted());
        JPanel text = new JPanel(new BorderLayout(0, 6));
        text.setOpaque(false);
        text.add(emptyTitle, BorderLayout.NORTH);
        text.add(sub, BorderLayout.CENTER);
        empty.add(bell, BorderLayout.NORTH);
        empty.add(text, BorderLayout.CENTER);
        JPanel emptyWrap = new JPanel(new BorderLayout());
        emptyWrap.add(empty, BorderLayout.NORTH);

        list.add(sp, "table");
        list.add(emptyWrap, "empty");
        return list;
    }

    @Override public String title() { return "Reminders"; }

    @Override
    public void render() {
        if (when.getText().isBlank()) when.setText(LocalDate.now().plusDays(1).atTime(LocalTime.of(18, 0)).format(WHEN));
        List<Reminder> all = facade.listReminders(ctx.studentId);
        shown = all.stream()
                .filter(r -> showAll.isSelected() || r.getStatus() == ReminderStatus.PENDING)
                .sorted(Comparator.comparing(Reminder::getDueAt)).toList();
        model.setRowCount(0);
        for (Reminder r : shown) {
            model.addRow(new Object[]{r.getDueAt().format(WHEN), r.getRecurrence(), r.getStatus(), r.getMessage()});
        }
        updateSummary(all);
        emptyTitle.setText(all.isEmpty() ? "No reminders yet" : "No upcoming reminders");
        listLayout.show(list, shown.isEmpty() ? "empty" : "table");
    }

    private void updateSummary(List<Reminder> all) {
        LocalDateTime now = LocalDateTime.now();
        List<Reminder> pending = all.stream().filter(r -> r.getStatus() == ReminderStatus.PENDING).toList();
        long overdue = pending.stream().filter(r -> !r.getDueAt().isAfter(now)).count();
        Optional<Reminder> next = pending.stream().filter(r -> r.getDueAt().isAfter(now)).min(Comparator.comparing(Reminder::getDueAt));
        StringBuilder sb = new StringBuilder("<html><b>").append(pending.size()).append("</b> upcoming");
        if (overdue > 0) {
            sb.append(" &nbsp;&middot;&nbsp; <font color='").append(Theme.hex(Theme.WARNING.darker())).append("'><b>")
              .append(overdue).append(" due now</b></font>");
        }
        next.ifPresent(r -> sb.append(" &nbsp;&middot;&nbsp; next ").append(relative(r.getDueAt(), now)).append(": <i>")
                .append(QuestionPanel.escape(r.getMessage())).append("</i>"));
        summary.setText(sb.append("</html>").toString());
    }

    public void createReminder() {
        attempt(() -> {
            Course c = ctx.course();
            LocalDateTime at = parseWhen(when.getText());
            Recurrence rec = (Recurrence) repeat.getSelectedItem();
            String text = message.getText();
            facade.scheduleReminder(new Reminder(Ids.newId("r"), ctx.studentId, c == null ? null : c.getId(), text, at, rec));
            String status = "Reminder added";
            int lead = LEAD_MINUTES[before.getSelectedIndex()];
            if (lead > 0) {
                LocalDateTime early = at.minusMinutes(lead);
                if (early.isAfter(LocalDateTime.now())) {
                    facade.scheduleReminder(new Reminder(Ids.newId("r"), ctx.studentId, c == null ? null : c.getId(),
                            earlyMessage(lead, text), early, rec));
                    status = "Reminder added, with an early reminder " + leadLabel(lead) + " before";
                } else {
                    status = "Reminder added (the early reminder would be in the past, so it was skipped)";
                }
            }
            message.setText("");
            render();
            ctx.status(status);
        });
    }

    /** "In 15 min: Review the observer pattern". */
    static String earlyMessage(int leadMinutes, String text) {
        return "In " + leadLabel(leadMinutes) + ": " + text;
    }

    static String leadLabel(int minutes) {
        return minutes >= 24 * 60 ? (minutes / (24 * 60)) + (minutes == 24 * 60 ? " day" : " days")
                : minutes >= 60 ? (minutes / 60) + (minutes == 60 ? " hour" : " hours") : minutes + " min";
    }

    /** Reminders 7, 3 and 1 days before the selected course's exam (18:00), skipping past and duplicate ones. */
    public void createExamReminders() {
        Course c = requireCourse();
        if (c == null) return;
        if (c.getExamDate() == null) {
            showError("This course has no exam date. Set it in the Courses tab first.");
            return;
        }
        attempt(() -> {
            List<String> existing = facade.listReminders(ctx.studentId).stream()
                    .filter(r -> r.getStatus() == ReminderStatus.PENDING).map(Reminder::getMessage).toList();
            int added = 0;
            for (int days : new int[]{7, 3, 1}) {
                LocalDateTime at = c.getExamDate().minusDays(days).atTime(18, 0);
                if (!at.isAfter(LocalDateTime.now())) continue;
                String text = examMessage(c.getCode(), days);
                if (existing.contains(text)) continue;
                facade.scheduleReminder(new Reminder(Ids.newId("r"), ctx.studentId, c.getId(), text, at, Recurrence.NONE));
                added++;
            }
            render();
            ctx.status(added == 0 ? "Exam reminders are already set (or the exam is too close)"
                    : added + " exam reminder(s) added before " + c.getExamDate());
        });
    }

    static String examMessage(String courseCode, int daysBefore) {
        return courseCode + " exam " + (daysBefore == 1 ? "tomorrow" : "in " + daysBefore + " days")
                + (daysBefore == 1 ? ": a light review of your weak topics, then rest." : ": review your weak topics and take a practice quiz.");
    }

    public void cancelReminder(String reminderId) {
        attempt(() -> {
            facade.cancelReminder(reminderId);
            render();
        });
    }

    private void cancelSelected() {
        int i = table.getSelectedRow();
        if (i < 0 || i >= shown.size()) {
            showError("Select a reminder first.");
            return;
        }
        cancelReminder(shown.get(i).getId());
    }

    private void dispatch() {
        attempt(() -> {
            int sent = facade.dispatchDueReminders();
            ctx.status(sent + " reminder(s) delivered");
            render();
        });
    }

    static LocalDateTime parseWhen(String s) {
        try {
            return LocalDateTime.parse(s.trim(), WHEN);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Time must look like 2026-11-05 18:30");
        }
    }

    // ------------------------------------------------------------------ presentation helpers

    static String repeatLabel(Recurrence r) {
        return switch (r) {
            case NONE -> "Once";
            case DAILY -> "Every day";
            case WEEKLY -> "Every week";
        };
    }

    /** "in 25 min", "in 3 h", "in 2 days", "5 min ago". */
    static String relative(LocalDateTime t, LocalDateTime now) {
        long min = Duration.between(now, t).toMinutes();
        long abs = Math.abs(min);
        String amount = abs < 1 ? "now" : abs < 60 ? abs + " min" : abs < 48 * 60 ? (abs + 30) / 60 + " h" : (abs + 720) / 1440 + " days";
        if (amount.equals("now")) return "now";
        return min >= 0 ? "in " + amount : amount + " ago";
    }

    private Reminder at(int row) {
        return row >= 0 && row < shown.size() ? shown.get(row) : null;
    }

    private boolean faded(Reminder r) {
        return r != null && r.getStatus() != ReminderStatus.PENDING;
    }

    /** Two lines: the day and time in bold, how far away it is underneath. */
    private final class WhenRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, false, row, col);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
            Reminder r = at(row);
            if (r == null) return this;
            LocalDateTime now = LocalDateTime.now();
            LocalDate d = r.getDueAt().toLocalDate();
            String day = d.equals(now.toLocalDate()) ? "Today" : d.equals(now.toLocalDate().plusDays(1)) ? "Tomorrow" : DAY.format(d);
            boolean overdue = r.getStatus() == ReminderStatus.PENDING && !r.getDueAt().isAfter(now);
            String sub = r.getStatus() == ReminderStatus.PENDING ? relative(r.getDueAt(), now) : r.getStatus() == ReminderStatus.SENT ? "delivered" : "cancelled";
            String subColor = Theme.hex(sel ? getForeground() : overdue ? Theme.WARNING.darker() : Theme.muted());
            setText("<html><b>" + day + "</b> &middot; " + r.getDueAt().format(TIME) + "<br><font color='" + subColor
                    + "' style='font-size:10px'>" + sub + "</font></html>");
            if (!sel) setForeground(faded(r) ? Theme.muted() : Theme.text());
            return this;
        }
    }

    private final class MessageRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, false, row, col);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            Reminder r = at(row);
            String text = String.valueOf(value);
            // Plan reminders read "Study plan today: A, B": show the label muted and the topics in bold.
            int colon = text.indexOf(": ");
            if (text.startsWith("Study plan today") && colon > 0) {
                setText("<html><font color='" + Theme.hex(sel ? getForeground() : Theme.muted()) + "'>" + QuestionPanel.escape(text.substring(0, colon + 1))
                        + "</font> <b>" + QuestionPanel.escape(text.substring(colon + 2)) + "</b></html>");
            } else {
                setText("<html><b>" + QuestionPanel.escape(text) + "</b></html>");
            }
            setToolTipText(text);
            if (!sel) setForeground(faded(r) ? Theme.muted() : Theme.text());
            return this;
        }
    }

    /** A rounded, colour-coded tag: the repeat rule, or the status (upcoming, due now, sent, cancelled). */
    private final class TagRenderer extends DefaultTableCellRenderer {
        private final boolean repeatColumn;
        private Color tone = Theme.ACCENT;
        private Color rowBg;

        TagRenderer(boolean repeatColumn) {
            this.repeatColumn = repeatColumn;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, false, row, col);
            Reminder r = at(row);
            rowBg = sel ? t.getSelectionBackground() : t.getBackground();
            setOpaque(false);
            setFont(t.getFont().deriveFont(Font.BOLD, 12f));
            setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 10));
            if (repeatColumn) {
                Recurrence rec = (Recurrence) value;
                setText(rec == Recurrence.NONE ? "Once" : "↻ " + repeatLabel(rec));
                tone = rec == Recurrence.NONE ? Theme.muted() : new Color(0x0D9488);
            } else {
                boolean overdue = r != null && r.getStatus() == ReminderStatus.PENDING && !r.getDueAt().isAfter(LocalDateTime.now());
                switch ((ReminderStatus) value) {
                    case PENDING -> {
                        setText(overdue ? "● Due now" : "● Upcoming");
                        tone = overdue ? Theme.WARNING.darker() : Theme.ACCENT;
                    }
                    case SENT -> {
                        setText("✓ Sent");
                        tone = Theme.SUCCESS;
                    }
                    case CANCELLED -> {
                        setText("✕ Cancelled");
                        tone = Theme.muted();
                    }
                }
            }
            setForeground(tone);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(rowBg);
            g.fillRect(0, 0, getWidth(), getHeight());
            int w = g.getFontMetrics(getFont()).stringWidth(getText()) + 22;
            int h = 26, y = (getHeight() - h) / 2;
            g.setColor(Theme.tint(tone, 0.15));
            g.fillRoundRect(11, y, w, h, h, h);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    JTable table() { return table; }
    JTextField whenField() { return when; }
    JTextField messageField() { return message; }
    JComboBox<String> beforeBox() { return before; }
}
