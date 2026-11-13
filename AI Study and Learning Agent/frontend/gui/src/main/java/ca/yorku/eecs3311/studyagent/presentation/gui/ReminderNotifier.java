package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.integration.notification.DesktopNotificationGateway;
import ca.yorku.eecs3311.studyagent.integration.notification.NotificationGateway;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Delivers reminders so the student actually sees them: the operating-system notification (which macOS may
 * silently hide, e.g. in Focus mode or when Script Editor notifications are off) plus an in-app pop-up card in the
 * top-right corner of the screen, a sound and a bouncing Dock icon. Reminders of a student who is not signed in
 * are not delivered yet; they stay pending until that student signs in.
 */
final class ReminderNotifier implements NotificationGateway {
    private static final int VISIBLE_MS = 20_000;
    private static final List<JWindow> open = new ArrayList<>();
    private static volatile String signedInStudent;
    private static volatile Runnable openReminders = () -> { };

    private final NotificationGateway system = new DesktopNotificationGateway();

    /** Called when a student signs in (or out, with null) and with the action that opens the Reminders tab. */
    static void signedIn(String studentId, Runnable showReminders) {
        signedInStudent = studentId;
        openReminders = showReminders == null ? () -> { } : showReminders;
    }

    @Override
    public boolean send(String title, String body, String recipient) {
        if (signedInStudent == null || !signedInStudent.equals(recipient)) return false;   // keep it pending
        system.send(title, body, recipient);
        if (!GraphicsEnvironment.isHeadless()) SwingUtilities.invokeLater(() -> popUp(title, body));
        return true;
    }

    /** Shows the pop-up card, rings and bounces the Dock icon. Must run on the event thread. */
    static void popUp(String title, String body) {
        Toolkit.getDefaultToolkit().beep();
        try {
            if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.USER_ATTENTION)) {
                Taskbar.getTaskbar().requestUserAttention(true, false);
            }
        } catch (RuntimeException ignored) {
            // attention request is best effort
        }

        JWindow w = new JWindow();
        w.setAlwaysOnTop(true);
        w.setFocusableWindowState(false);
        w.setBackground(new Color(0, 0, 0, 0));

        JPanel card = new JPanel(new BorderLayout(12, 4)) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Theme.card());
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g.setColor(Theme.ACCENT);
                g.fillRoundRect(0, 0, 8, getHeight() - 1, 8, 8);
                g.fillRect(4, 0, 4, getHeight() - 1);
                g.setColor(Theme.border());
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 22, 12, 14));

        JLabel icon = new JLabel("⏰");
        icon.setFont(icon.getFont().deriveFont(26f));
        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 14f));
        heading.setForeground(Theme.ACCENT);
        JLabel text = new JLabel("<html><div style='width:250px'>" + QuestionPanel.escape(body) + "</div></html>");
        text.setForeground(Theme.text());
        JPanel words = new JPanel(new BorderLayout(0, 4));
        words.setOpaque(false);
        words.add(heading, BorderLayout.NORTH);
        words.add(text, BorderLayout.CENTER);

        JButton openButton = new JButton("Open");
        openButton.addActionListener(e -> {
            close(w);
            openReminders.run();
        });
        JButton dismiss = new JButton("Dismiss");
        dismiss.addActionListener(e -> close(w));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        buttons.add(dismiss);
        buttons.add(openButton);

        card.add(icon, BorderLayout.WEST);
        card.add(words, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                close(w);
                openReminders.run();
            }
        });
        w.setContentPane(card);
        w.pack();

        open.add(w);
        layoutOpen();
        w.setVisible(true);
        Timer t = new Timer(VISIBLE_MS, e -> close(w));
        t.setRepeats(false);
        t.start();
    }

    private static void close(JWindow w) {
        if (!open.remove(w)) return;
        w.dispose();
        layoutOpen();
    }

    /** Stacks the open pop-ups under each other in the top-right corner of the screen. */
    private static void layoutOpen() {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int y = screen.y + 16;
        for (JWindow w : open) {
            w.setLocation(screen.x + screen.width - w.getWidth() - 16, y);
            y += w.getHeight() + 10;
        }
    }
}
