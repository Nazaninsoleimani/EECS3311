package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.config.AppConfig;
import ca.yorku.eecs3311.studyagent.config.AppContext;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.integration.notification.DesktopNotificationGateway;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/** Desktop GUI entry point: one window with a course selector, a tab per view and a status bar. */
public class StudyAgentGui {
    private final GuiContext ctx;
    private final List<BaseView> views;
    private final JTabbedPane tabs = new JTabbedPane();
    private final JComboBox<Course> courseBox = new JComboBox<>();
    private final JLabel status = new JLabel("Ready");
    private boolean updatingBox;

    public StudyAgentGui(GuiContext ctx) {
        this.ctx = ctx;
        this.views = createViews(ctx);
        views.forEach(v -> tabs.addTab(v.title(), v));
        tabs.addChangeListener(e -> renderSelected());
        courseBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                String text = value instanceof Course c ? c.getCode() + " - " + c.getTitle() : "(no courses yet)";
                return super.getListCellRendererComponent(list, text, index, sel, focus);
            }
        });
        courseBox.addActionListener(e -> {
            if (!updatingBox && courseBox.getSelectedItem() instanceof Course c) ctx.selectCourse(c);
        });
        ctx.onCourseListChanged(this::fillCourseBox);
        ctx.onCourseChanged(c -> {
            updatingBox = true;
            courseBox.setSelectedItem(c);
            updatingBox = false;
            renderSelected();
        });
        ctx.onDataChanged(this::renderSelected);
        ctx.setStatusLine(text -> SwingUtilities.invokeLater(() -> status.setText(text)));
    }

    /** Every view of the application, in tab order. */
    static List<BaseView> createViews(GuiContext ctx) {
        List<BaseView> list = new ArrayList<>();
        list.add(new CourseView(ctx));
        list.add(new MaterialView(ctx));
        list.add(new TutorChatView(ctx));
        return list;
    }

    JFrame buildFrame() {
        JFrame frame = new JFrame("AI Study and Learning Agent");
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setBorder(BorderFactory.createEmptyBorder(8, 10, 4, 10));
        JPanel left = new JPanel();
        left.add(new JLabel("Course:"));
        courseBox.setPreferredSize(new Dimension(320, courseBox.getPreferredSize().height));
        left.add(courseBox);
        top.add(left, BorderLayout.WEST);
        JLabel llm = new JLabel(ctx.facade.isLlmAvailable() ? "AI: connected" : "AI: not configured (see README)");
        top.add(llm, BorderLayout.EAST);
        status.setBorder(BorderFactory.createEmptyBorder(4, 10, 6, 10));
        frame.add(top, BorderLayout.NORTH);
        frame.add(tabs, BorderLayout.CENTER);
        frame.add(status, BorderLayout.SOUTH);
        frame.setSize(1100, 760);
        frame.setLocationRelativeTo(null);
        ctx.reloadCourses();
        renderSelected();
        return frame;
    }

    private void fillCourseBox(List<Course> courses) {
        updatingBox = true;
        courseBox.removeAllItems();
        courses.forEach(courseBox::addItem);
        updatingBox = false;
    }

    private void renderSelected() {
        Component c = tabs.getSelectedComponent();
        if (c instanceof BaseView v) {
            try {
                v.render();
            } catch (RuntimeException e) {
                status.setText("Could not load " + v.title() + ": " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // the default look and feel is fine
        }
        AppContext app;
        try {
            app = AppContext.create(AppConfig.load(), new DesktopNotificationGateway());
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(null, "Cannot start: " + e.getMessage(), "Study Agent", JOptionPane.ERROR_MESSAGE);
            return;
        }
        app.facade().startReminderTimer(app.config().getInt("REMINDER_CHECK_SECONDS", 60));
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new StudyAgentGui(new GuiContext(app.facade(), app.studentId())).buildFrame();
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    app.close();
                    System.exit(0);
                }
            });
            frame.setVisible(true);
        });
    }

    List<BaseView> views() { return views; }
}
