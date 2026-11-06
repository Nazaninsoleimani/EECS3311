package ca.yorku.eecs3311.studyagent.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Rounded panel with a subtle border, the building block of the dashboard. */
class Card extends JPanel {
    private final Color tint;

    Card(LayoutManager layout) {
        this(layout, null);
    }

    /** A card whose background is tinted with the given colour (used for banners). */
    Card(LayoutManager layout, Color tint) {
        super(layout);
        this.tint = tint;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
    }

    /** Card with a small bold title above its content. */
    static Card titled(String title, java.awt.Component content) {
        Card c = new Card(new BorderLayout(0, 8));
        JLabel l = new JLabel(title);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13.5f));
        c.add(l, BorderLayout.NORTH);
        c.add(content, BorderLayout.CENTER);
        return c;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(tint == null ? Theme.card() : Theme.tint(tint, 0.14));
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        g.setColor(tint == null ? Theme.border() : Theme.tint(tint, 0.45));
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        g.dispose();
        super.paintComponent(g0);
    }

    /** Rounded coloured label, e.g. the AI status badge. */
    static class Pill extends JLabel {
        private Color color;

        Pill(String text, Color color) {
            super(text);
            this.color = color;
            setForeground(color);
            setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
            setFont(getFont().deriveFont(Font.BOLD, 12f));
        }

        void set(String text, Color color) {
            setText(text);
            this.color = color;
            setForeground(color);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Theme.tint(color, 0.18));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g.dispose();
            super.paintComponent(g0);
        }
    }
}
