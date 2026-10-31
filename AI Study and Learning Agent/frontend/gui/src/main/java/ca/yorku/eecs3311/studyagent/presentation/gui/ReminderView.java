package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.Ids;
import ca.yorku.eecs3311.studyagent.domain.Recurrence;
import ca.yorku.eecs3311.studyagent.domain.Reminder;
import ca.yorku.eecs3311.studyagent.domain.ReminderStatus;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** F14: schedule, list and cancel study reminders; due ones pop up as desktop notifications. */
public class ReminderView extends BaseView {
    static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JTextField when = new JTextField(14);
    private final JComboBox<Recurrence> repeat = new JComboBox<>(Recurrence.values());
    private final JTextField message = new JTextField(30);
    private final JCheckBox showAll = new JCheckBox("Show sent and cancelled");
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"When", "Repeats", "Status", "Message"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<Reminder> shown = List.of();

    public ReminderView(GuiContext ctx) {
        super(ctx);
        table.getColumnModel().getColumn(3).setPreferredWidth(420);
        showAll.addActionListener(e -> render());
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading("Reminders"), BorderLayout.NORTH);
        top.add(row(new JLabel("When (yyyy-mm-dd hh:mm)"), when, new JLabel("Repeat"), repeat, new JLabel("Message"), message,
                button("Add reminder", this::createReminder)), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(scroll(table), BorderLayout.CENTER);
        add(row(button("Cancel selected", this::cancelSelected), button("Send due reminders now", this::dispatch), showAll), BorderLayout.SOUTH);
    }

    @Override public String title() { return "Reminders"; }

    @Override
    public void render() {
        if (when.getText().isBlank()) when.setText(LocalDate.now().plusDays(1).atTime(LocalTime.of(18, 0)).format(WHEN));
        shown = facade.listReminders(ctx.studentId).stream()
                .filter(r -> showAll.isSelected() || r.getStatus() == ReminderStatus.PENDING)
                .sorted(Comparator.comparing(Reminder::getDueAt)).toList();
        model.setRowCount(0);
        for (Reminder r : shown) {
            model.addRow(new Object[]{r.getDueAt().format(WHEN), r.getRecurrence(), r.getStatus(), r.getMessage()});
        }
    }

    public void createReminder() {
        attempt(() -> {
            Course c = ctx.course();
            facade.scheduleReminder(new Reminder(Ids.newId("r"), ctx.studentId, c == null ? null : c.getId(), message.getText(),
                    parseWhen(when.getText()), (Recurrence) repeat.getSelectedItem()));
            message.setText("");
            render();
            ctx.status("Reminder added");
        });
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

    JTable table() { return table; }
    JTextField whenField() { return when; }
    JTextField messageField() { return message; }
}
