package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.application.LearningFacade;
import ca.yorku.eecs3311.studyagent.domain.Student;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * First screen of the GUI: sign in to an existing account or create a new one. Each account sees only
 * its own courses, progress and history. On success the signed-in student is handed to onSuccess.
 */
public class AuthPanel extends JPanel {
    private final LearningFacade facade;
    private final Consumer<Student> onSuccess;
    private final JLabel title = new JLabel();
    private final JLabel subtitle = new JLabel();
    private final JTextField name = new JTextField();
    private final JTextField email = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JPasswordField confirm = new JPasswordField();
    private final JLabel error = new JLabel(" ");
    private final JButton submit = new JButton();
    private final JLabel switchLink = new JLabel();
    private final JPanel nameRow;
    private final JPanel confirmRow;
    private boolean createMode;

    public AuthPanel(LearningFacade facade, Consumer<Student> onSuccess) {
        super(new GridLayout(1, 2));
        this.facade = facade;
        this.onSuccess = onSuccess;
        add(new BrandPanel());

        name.putClientProperty("JTextField.placeholderText", "Your name");
        email.putClientProperty("JTextField.placeholderText", "you@example.com");
        password.putClientProperty("JTextField.placeholderText", "At least 8 characters");
        confirm.putClientProperty("JTextField.placeholderText", "Repeat the password");
        password.putClientProperty("JPasswordField.showRevealButton", true);
        confirm.putClientProperty("JPasswordField.showRevealButton", true);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        subtitle.setForeground(Theme.muted());
        error.setForeground(Theme.DANGER);
        submit.putClientProperty("FlatLaf.style", "background: #7C3AED; foreground: #FFFFFF; borderWidth: 0; focusWidth: 0; font: bold +1");
        submit.setPreferredSize(new Dimension(320, 40));
        submit.setMinimumSize(new Dimension(320, 40));
        submit.setMaximumSize(new Dimension(320, 40));
        submit.addActionListener(e -> submit());
        switchLink.setForeground(Theme.ACCENT);
        switchLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        switchLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { setCreateMode(!createMode); }
        });
        for (JTextField f : new JTextField[]{name, email, password, confirm}) f.addActionListener(e -> submit());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        nameRow = field("Name", name);
        confirmRow = field("Confirm password", confirm);
        form.add(left(title));
        form.add(Box.createVerticalStrut(4));
        form.add(left(subtitle));
        form.add(Box.createVerticalStrut(24));
        form.add(nameRow);
        form.add(field("Email", email));
        form.add(field("Password", password));
        form.add(confirmRow);
        form.add(left(error));
        form.add(Box.createVerticalStrut(8));
        form.add(left(submit));
        form.add(Box.createVerticalStrut(16));
        form.add(left(switchLink));
        form.setMaximumSize(new Dimension(340, 600));

        JPanel right = new JPanel(new GridBagLayout());
        right.setBorder(BorderFactory.createEmptyBorder(24, 40, 24, 40));
        right.add(form);
        add(right);
        setCreateMode(false);
    }

    void setCreateMode(boolean create) {
        createMode = create;
        title.setText(create ? "Create your account" : "Welcome back");
        subtitle.setText(create ? "Your courses, plans and progress stay private to your account."
                : "Sign in to continue studying.");
        submit.setText(create ? "Create account" : "Sign in");
        switchLink.setText(create ? "Already have an account?  Sign in" : "New here?  Create an account");
        nameRow.setVisible(create);
        confirmRow.setVisible(create);
        error.setText(" ");
        revalidate();
        repaint();
        (create ? name : email).requestFocusInWindow();
    }

    /** Creates the account or signs in; validation messages are shown under the form. */
    void submit() {
        char[] pw = password.getPassword();
        char[] pw2 = confirm.getPassword();
        try {
            Student s = createMode ? facade.createAccount(name.getText(), email.getText(), pw, pw2)
                    : facade.signIn(email.getText(), pw);
            error.setText(" ");
            password.setText("");
            confirm.setText("");
            onSuccess.accept(s);
        } catch (RuntimeException e) {
            error.setText(e.getMessage() == null ? "Something went wrong" : e.getMessage());
        } finally {
            Arrays.fill(pw, '\0');
            Arrays.fill(pw2, '\0');
        }
    }

    private static JPanel field(String label, JComponent input) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12.5f));
        p.add(l, BorderLayout.NORTH);
        input.setPreferredSize(new Dimension(320, 36));
        p.add(input, BorderLayout.CENTER);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(340, 80));
        return p;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    /** Purple welcome side with the app name and what it does. */
    private static final class BrandPanel extends JPanel {
        BrandPanel() {
            setLayout(new GridBagLayout());
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            JLabel name = new JLabel("Study Agent");
            name.setFont(name.getFont().deriveFont(Font.BOLD, 34f));
            name.setForeground(Color.WHITE);
            JLabel tag = new JLabel("Your AI study companion");
            tag.setFont(tag.getFont().deriveFont(16f));
            tag.setForeground(new Color(0xEDE9FE));
            text.add(name);
            text.add(Box.createVerticalStrut(6));
            text.add(tag);
            text.add(Box.createVerticalStrut(34));
            for (String line : new String[]{"Ask questions about your own lecture notes", "Get a study plan that fits your exam date",
                    "Practise with quizzes, flashcards and adaptive sessions", "See your progress and weak topics at a glance"}) {
                JLabel l = new JLabel("✓   " + line);
                l.setForeground(Color.WHITE);
                l.setFont(l.getFont().deriveFont(14f));
                text.add(l);
                text.add(Box.createVerticalStrut(12));
            }
            add(text);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setPaint(new GradientPaint(0, 0, new Color(0x5B21B6), getWidth(), getHeight(), new Color(0xA855F7)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }
    }

    JTextField nameField() { return name; }
    JTextField emailField() { return email; }
    JPasswordField passwordField() { return password; }
    JPasswordField confirmField() { return confirm; }
    JLabel errorLabel() { return error; }
    boolean isCreateMode() { return createMode; }
}
