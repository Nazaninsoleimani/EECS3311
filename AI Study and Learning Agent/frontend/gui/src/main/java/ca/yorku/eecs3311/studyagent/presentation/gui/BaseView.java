package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.application.LearningFacade;
import ca.yorku.eecs3311.studyagent.domain.Course;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/**
 * Base class of every GUI view. Views talk only to the LearningFacade; slow calls (anything that
 * may reach the LLM) run off the event thread through runAsync so the window stays responsive.
 */
public abstract class BaseView extends JPanel {
    protected final GuiContext ctx;
    protected final LearningFacade facade;
    private int busy;

    protected BaseView(GuiContext ctx) {
        super(new BorderLayout(8, 8));
        this.ctx = ctx;
        this.facade = ctx.facade;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /** Tab title. */
    public abstract String title();

    /** Reloads what the view shows from the facade; called when its tab opens or the course changes. */
    public abstract void render();

    /** The selected course, or null after telling the user to pick one. */
    protected Course requireCourse() {
        Course c = ctx.course();
        if (c == null) showError("Create or select a course first (Courses tab).");
        return c;
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Study Agent", JOptionPane.ERROR_MESSAGE);
    }

    protected void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Study Agent", JOptionPane.INFORMATION_MESSAGE);
    }

    protected boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Study Agent", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /** Runs work in the background, then onDone on the event thread; failures are shown as error dialogs. */
    protected <T> void runAsync(String busyText, Callable<T> work, Consumer<T> onDone) {
        runAsync(busyText, work, onDone, () -> {});
    }

    /** As above; always runs afterwards, after success or failure (e.g. to re-enable a button). */
    protected <T> void runAsync(String busyText, Callable<T> work, Consumer<T> onDone, Runnable always) {
        busy++;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ctx.status(busyText + "...");
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                if (--busy == 0) setCursor(Cursor.getDefaultCursor());
                try {
                    T result = get();
                    ctx.status("Ready");
                    onDone.accept(result);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    ctx.status("Failed: " + cause.getMessage());
                    showError(cause.getMessage() == null ? cause.toString() : cause.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    always.run();
                }
            }
        }.execute();
    }

    /** Runs a quick facade call on the event thread, showing any validation error. */
    protected void attempt(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            showError(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    protected static JTextArea readOnlyArea() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(a.getFont().deriveFont(13f));
        return a;
    }

    protected static JScrollPane scroll(Component c) {
        return new JScrollPane(c);
    }

    protected static JPanel row(Component... components) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        for (Component c : components) p.add(c);
        return p;
    }

    protected static JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        return b;
    }

    protected static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
        return l;
    }
}
