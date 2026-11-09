package ca.yorku.eecs3311.studyagent.presentation.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.function.DoubleConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.Timer;

/**
 * Small, short UI animations driven by a Swing timer (about 60 frames per second, ease-out timing):
 * fades between screens, counting numbers, filling bars and a shake for wrong input. Without a
 * display (tests) every animation jumps straight to its final state.
 */
final class Animations {
    private static final int FRAME_MS = 16;
    private static final Pattern NUMBER = Pattern.compile("^(\\d+)(%?)$");

    private Animations() {}

    /** Calls onFrame with progress 0..1 (eased) for the given duration, then done. */
    static Timer animate(int durationMs, DoubleConsumer onFrame, Runnable done) {
        if (GraphicsEnvironment.isHeadless() || durationMs <= 0) {
            onFrame.accept(1.0);
            if (done != null) done.run();
            return null;
        }
        long start = System.nanoTime();
        Timer timer = new Timer(FRAME_MS, null);
        timer.addActionListener(e -> {
            double t = Math.min(1.0, (System.nanoTime() - start) / 1e6 / durationMs);
            onFrame.accept(easeOut(t));
            if (t >= 1.0) {
                timer.stop();
                if (done != null) done.run();
            }
        });
        timer.setInitialDelay(0);
        timer.start();
        return timer;
    }

    static double easeOut(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    /** A label showing "42" or "67%" counts up from 0 to its value. Other text is left as it is. */
    static void countUp(JLabel label, int durationMs) {
        Matcher m = NUMBER.matcher(label.getText());
        if (!m.matches()) return;
        int target = Integer.parseInt(m.group(1));
        String suffix = m.group(2);
        animate(durationMs, p -> label.setText((int) Math.round(target * p) + suffix), null);
    }

    /** A progress bar fills from 0 to its current value, with its text following. */
    static void fill(JProgressBar bar, int durationMs) {
        int target = bar.getValue();
        animate(durationMs, p -> {
            int v = (int) Math.round(target * p);
            bar.setValue(v);
            bar.setString(v + "%");
        }, null);
    }

    /** The window content fades in from the background colour (used when switching screens). */
    static void fadeIn(JRootPane root, Color background, int durationMs) {
        if (root == null) return;
        Overlay overlay = Overlay.install(root);
        overlay.color = background;
        overlay.image = null;
        run(overlay, durationMs);
    }

    /** Takes a picture of the window, applies the change, then fades the picture out (theme switch). */
    static void crossFade(JRootPane root, Runnable change, int durationMs) {
        if (root == null || GraphicsEnvironment.isHeadless() || root.getWidth() <= 0) {
            change.run();
            return;
        }
        BufferedImage before = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = before.createGraphics();
        root.paint(g);
        g.dispose();
        change.run();
        Overlay overlay = Overlay.install(root);
        overlay.image = before;
        overlay.color = null;
        run(overlay, durationMs);
    }

    /** Short horizontal shake, e.g. of a form after a wrong password. */
    static void shake(Component c) {
        Point origin = c.getLocation();
        animate(360, p -> {
            int dx = (int) Math.round(Math.sin(p * Math.PI * 6) * 9 * (1 - p));
            c.setLocation(origin.x + dx, origin.y);
        }, () -> c.setLocation(origin));
    }

    private static void run(Overlay overlay, int durationMs) {
        overlay.alpha = 1f;
        overlay.setVisible(true);
        animate(durationMs, p -> {
            overlay.alpha = (float) (1 - p);
            overlay.repaint();
        }, () -> {
            overlay.setVisible(false);
            overlay.image = null;
        });
    }

    /** Glass pane that paints a colour or a picture with decreasing opacity over the window. */
    private static final class Overlay extends JComponent {
        Color color;
        BufferedImage image;
        float alpha;

        static Overlay install(JRootPane root) {
            if (root.getGlassPane() instanceof Overlay o) return o;
            Overlay o = new Overlay();
            root.setGlassPane(o);
            return o;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
            if (image != null) {
                g.drawImage(image, 0, 0, null);
            } else if (color != null) {
                g.setColor(color);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            g.dispose();
        }
    }
}
