package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** F01: create, edit, delete and select course workspaces. */
public class CourseView extends BaseView {
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Code", "Title", "Exam date", "Days left", "Id"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField code = new JTextField(10);
    private final JTextField title = new JTextField(24);
    private final JTextField exam = new JTextField(10);
    private List<Course> shown = List.of();

    public CourseView(GuiContext ctx) {
        super(ctx);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            int i = table.getSelectedRow();
            if (e.getValueIsAdjusting() || i < 0 || i >= shown.size()) return;
            Course c = shown.get(i);
            code.setText(c.getCode());
            title.setText(c.getTitle());
            exam.setText(c.getExamDate() == null ? "" : c.getExamDate().toString());
            ctx.selectCourse(c);
        });
        JPanel form = new JPanel(new GridLayout(0, 1));
        form.add(row(new JLabel("Code"), code, new JLabel("Title"), title, new JLabel("Exam (yyyy-mm-dd, optional)"), exam));
        form.add(row(button("Create course", this::create), button("Save changes", this::update),
                button("Delete course", this::delete), button("Clear", this::clear)));
        add(heading("Your courses"), BorderLayout.NORTH);
        add(scroll(table), BorderLayout.CENTER);
        add(form, BorderLayout.SOUTH);
        ctx.onCourseListChanged(list -> render());
    }

    @Override public String title() { return "Courses"; }

    @Override
    public void render() {
        shown = ctx.courses();
        model.setRowCount(0);
        for (Course c : shown) {
            model.addRow(new Object[]{c.getCode(), c.getTitle(), c.getExamDate() == null ? "" : c.getExamDate(),
                    c.getExamDate() == null ? "" : c.daysUntilExam(), c.getId()});
        }
        Course sel = ctx.course();
        for (int i = 0; sel != null && i < shown.size(); i++) {
            if (shown.get(i).getId().equals(sel.getId())) table.getSelectionModel().setSelectionInterval(i, i);
        }
    }

    private void create() {
        attempt(() -> {
            Course c = facade.createCourse(ctx.studentId, code.getText(), title.getText(), examDate());
            ctx.reloadCourses();
            ctx.selectCourse(c);
            render();
            ctx.status("Created " + c.getCode());
        });
    }

    private void update() {
        Course c = requireCourse();
        if (c == null) return;
        attempt(() -> {
            LocalDate date = examDate();
            c.setCode(code.getText());
            c.setTitle(title.getText());
            c.setExamDate(date);
            facade.updateCourse(c);
            ctx.reloadCourses();
            ctx.status("Saved " + c.getCode());
        });
    }

    private void delete() {
        Course c = requireCourse();
        if (c == null || !confirm("Delete " + c.getCode() + " with all its materials, quizzes, plans and progress?")) return;
        attempt(() -> {
            facade.deleteCourse(c.getId());
            clear();
            ctx.reloadCourses();
            ctx.status("Deleted " + c.getCode());
        });
    }

    private void clear() {
        code.setText("");
        title.setText("");
        exam.setText("");
        table.clearSelection();
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

    JTextField codeField() { return code; }
    JTextField titleField() { return title; }
    JTable table() { return table; }
}
