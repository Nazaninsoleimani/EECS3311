package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.config.AppConfig;
import ca.yorku.eecs3311.studyagent.config.AppContext;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.integration.notification.DesktopNotificationGateway;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Desktop GUI entry point: a header with the course selector, AI status and theme switch, a sidebar
 * with one entry per view, and a status line.
 */
public class StudyAgentGui {
    private final GuiContext ctx;
    private final List<BaseView> views;
    private final JTabbedPane tabs = new JTabbedPane(SwingConstants.LEFT);
    private final JComboBox<Course> courseBox = new JComboBox<>();
    private final JLabel status = new JLabel("Ready");
    private final Card.Pill aiPill = new Card.Pill("", Theme.WARNING);
    private final JButton themeButton = new JButton();
    private boolean updatingBox;

    public StudyAgentGui(GuiContext ctx) {
        this.ctx = ctx;
        this.views = createViews(ctx);
        views.forEach(v -> tabs.addTab(" " + v.title(), NavIcon.forTitle(v.title()), v));
        styleSidebar();
        tabs.addChangeListener(e -> renderSelected());
        courseBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                String text = value instanceof Course c ? c.getCode() + "  ·  " + c.getTitle() : "No courses yet";
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
        ctx.onQuizRequested(concept -> show(QuizView.class));
        ctx.onNavigate(this::show);
        ctx.setStatusLine(text -> SwingUtilities.invokeLater(() -> status.setText(text)));
        updateAiPill();
    }

    /** Every view of the application, in sidebar order. */
    static List<BaseView> createViews(GuiContext ctx) {
        List<BaseView> list = new ArrayList<>();
        list.add(new DashboardView(ctx));
        list.add(new CourseView(ctx));
        list.add(new MaterialView(ctx));
        list.add(new TutorChatView(ctx));
        list.add(new StudyPlannerView(ctx));
        list.add(new QuizView(ctx));
        list.add(new FlashcardView(ctx));
        list.add(new LearningSessionView(ctx));
        list.add(new ProgressDashboardView(ctx));
        list.add(new ConceptMapView(ctx));
        list.add(new ReminderView(ctx));
        list.add(new HistoryView(ctx));
        return list;
    }

    /** FlatLaf client properties that turn the left tabs into a sidebar; ignored by other look and feels. */
    private void styleSidebar() {
        tabs.putClientProperty("JTabbedPane.tabType", "underlined");
        tabs.putClientProperty("JTabbedPane.tabAreaAlignment", "leading");
        tabs.putClientProperty("JTabbedPane.tabAlignment", SwingConstants.LEADING);
        tabs.putClientProperty("JTabbedPane.minimumTabWidth", 190);
        tabs.putClientProperty("JTabbedPane.tabHeight", 42);
        tabs.putClientProperty("JTabbedPane.tabInsets", new Insets(0, 16, 0, 16));
        tabs.putClientProperty("JTabbedPane.tabAreaInsets", new Insets(10, 0, 10, 0));
        tabs.putClientProperty("JTabbedPane.showTabSeparators", false);
        tabs.putClientProperty("JTabbedPane.hasFullBorder", false);
    }

    JFrame buildFrame() {
        JFrame frame = new JFrame("Study Agent - AI Study and Learning Agent");
        frame.add(header(), BorderLayout.NORTH);
        frame.add(tabs, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.border()),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        status.setForeground(Theme.muted());
        frame.add(status, BorderLayout.SOUTH);
        frame.setSize(1320, 860);
        frame.setMinimumSize(new Dimension(1080, 680));
        frame.setLocationRelativeTo(null);
        ctx.reloadCourses();
        renderSelected();
        return frame;
    }

    private JPanel header() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border()),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)));

        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setOpaque(false);
        JLabel name = new JLabel("Study Agent");
        name.setFont(name.getFont().deriveFont(Font.BOLD, 18f));
        name.setForeground(Theme.ACCENT);
        JLabel tagline = new JLabel("Your AI study companion");
        tagline.setForeground(Theme.muted());
        tagline.setFont(tagline.getFont().deriveFont(11.5f));
        brand.add(name);
        brand.add(tagline);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        center.setOpaque(false);
        JLabel courseLabel = new JLabel("Course");
        courseLabel.setForeground(Theme.muted());
        courseBox.setPreferredSize(new Dimension(340, 32));
        center.add(Box.createHorizontalStrut(24));
        center.add(courseLabel);
        center.add(courseBox);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        right.setOpaque(false);
        aiPill.setToolTipText("How to switch the AI on");
        aiPill.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showAiHelp(bar); }
        });
        themeButton.putClientProperty("JButton.buttonType", "roundRect");
        themeButton.addActionListener(e -> {
            Theme.toggle();
            updateThemeButton();
            renderSelected();
        });
        updateThemeButton();
        right.add(aiPill);
        right.add(themeButton);

        bar.add(brand, BorderLayout.WEST);
        bar.add(center, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void updateAiPill() {
        if (ctx.facade.isLlmAvailable()) aiPill.set("●  AI connected", Theme.SUCCESS);
        else aiPill.set("●  AI off - click to switch on", Theme.WARNING);
    }

    private void updateThemeButton() {
        themeButton.setText(Theme.isDark() ? "Light mode" : "Dark mode");
    }

    static void showAiHelp(Component parent) {
        JOptionPane.showMessageDialog(parent, """
                <html><body style='width:380px'>
                <h3>Switching the AI on</h3>
                <ol>
                <li>Get a free key at <b>aistudio.google.com/apikey</b> (sign in with Google, then "Create API key").</li>
                <li>In the project folder, open the file <b>.env</b> (copy <b>.env.example</b> to <b>.env</b> if it does not exist).</li>
                <li>Replace <b>YOUR_LOCAL_API_KEY_HERE</b> on the <b>GEMINI_API_KEY=</b> line with your key and save.</li>
                <li>Close and restart Study Agent. The badge turns green.</li>
                </ol>
                <p>The key stays on your computer: <b>.env</b> is never committed to GitHub.</p>
                </body></html>""", "Study Agent - AI setup", JOptionPane.INFORMATION_MESSAGE);
    }

    private void show(Class<? extends BaseView> type) {
        views.stream().filter(type::isInstance).findFirst().ifPresent(tabs::setSelectedComponent);
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
        Theme.install(Theme.isDark());
        AppContext app;
        try {
            app = AppContext.create(AppConfig.load(), new DesktopNotificationGateway());
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(null, "Cannot start: " + e.getMessage(), "Study Agent", JOptionPane.ERROR_MESSAGE);
            return;
        }
        app.facade().startReminderTimer(app.config().getInt("REMINDER_CHECK_SECONDS", 60));
        new Thread(() -> {
            try {
                app.facade().retryPendingIndexing(app.studentId());
            } catch (RuntimeException e) {
                // still unavailable; the Materials view has a Retry indexing button
            }
        }, "pending-indexing").start();
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
    Component selectedView() { return tabs.getSelectedComponent(); }
}
