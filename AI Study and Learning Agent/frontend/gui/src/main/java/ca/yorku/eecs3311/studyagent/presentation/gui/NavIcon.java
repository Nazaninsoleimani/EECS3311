package ca.yorku.eecs3311.studyagent.presentation.gui;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/** Small line icons for the sidebar, drawn in the tab's text colour so they follow the theme. */
final class NavIcon implements Icon {
    enum Kind { DASHBOARD, COURSES, MATERIALS, TUTOR, PLAN, QUIZ, FLASHCARDS, SESSION, PROGRESS, MAP, REMINDERS, HISTORY }

    private static final int SIZE = 18;
    private final Kind kind;

    NavIcon(Kind kind) {
        this.kind = kind;
    }

    /** Icon for a view, chosen by its tab title. */
    static NavIcon forTitle(String title) {
        return new NavIcon(switch (title) {
            case "Dashboard" -> Kind.DASHBOARD;
            case "Courses" -> Kind.COURSES;
            case "Materials" -> Kind.MATERIALS;
            case "Tutor" -> Kind.TUTOR;
            case "Study Plan" -> Kind.PLAN;
            case "Quiz" -> Kind.QUIZ;
            case "Flashcards" -> Kind.FLASHCARDS;
            case "Learning Session" -> Kind.SESSION;
            case "Progress" -> Kind.PROGRESS;
            case "Concept Map" -> Kind.MAP;
            case "Reminders" -> Kind.REMINDERS;
            default -> Kind.HISTORY;
        });
    }

    @Override public int getIconWidth() { return SIZE; }

    @Override public int getIconHeight() { return SIZE; }

    @Override
    public void paintIcon(Component c, Graphics g0, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(x, y);
        g.setColor(c.getForeground());
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (kind) {
            case DASHBOARD -> {
                g.draw(rr(2, 2, 6, 6)); g.draw(rr(10, 2, 6, 6)); g.draw(rr(2, 10, 6, 6)); g.draw(rr(10, 10, 6, 6));
            }
            case COURSES -> {
                g.draw(rr(3, 2, 12, 14)); g.draw(new Line2D.Double(6, 2, 6, 16)); g.draw(new Line2D.Double(9, 6, 13, 6));
            }
            case MATERIALS -> {
                Path2D p = new Path2D.Double();
                p.moveTo(4, 2); p.lineTo(11, 2); p.lineTo(15, 6); p.lineTo(15, 16); p.lineTo(4, 16); p.closePath();
                g.draw(p); g.draw(new Line2D.Double(7, 9, 12, 9)); g.draw(new Line2D.Double(7, 12, 12, 12));
            }
            case TUTOR -> {
                Path2D p = new Path2D.Double();
                p.moveTo(3, 4); p.lineTo(15, 4); p.lineTo(15, 12); p.lineTo(8, 12); p.lineTo(5, 15); p.lineTo(5, 12); p.lineTo(3, 12); p.closePath();
                g.draw(p);
            }
            case PLAN -> {
                g.draw(rr(2, 4, 14, 12)); g.draw(new Line2D.Double(2, 8, 16, 8));
                g.draw(new Line2D.Double(6, 2, 6, 5)); g.draw(new Line2D.Double(12, 2, 12, 5));
            }
            case QUIZ -> {
                g.draw(new Ellipse2D.Double(2, 2, 14, 14));
                Path2D p = new Path2D.Double(); p.moveTo(6, 9); p.lineTo(8.3, 11.5); p.lineTo(12.5, 6.5); g.draw(p);
            }
            case FLASHCARDS -> {
                g.draw(rr(2, 5, 11, 11)); g.draw(new Line2D.Double(5, 2, 16, 2)); g.draw(new Line2D.Double(16, 2, 16, 13));
            }
            case SESSION -> {
                Path2D p = new Path2D.Double();
                p.moveTo(10, 2); p.lineTo(4, 10); p.lineTo(9, 10); p.lineTo(8, 16); p.lineTo(14, 8); p.lineTo(9, 8); p.closePath();
                g.draw(p);
            }
            case PROGRESS -> {
                g.draw(new Line2D.Double(4, 15, 4, 10)); g.draw(new Line2D.Double(9, 15, 9, 4)); g.draw(new Line2D.Double(14, 15, 14, 7));
                g.draw(new Line2D.Double(2, 16, 16, 16));
            }
            case MAP -> {
                g.draw(new Line2D.Double(5, 5, 13, 9)); g.draw(new Line2D.Double(5, 5, 6, 13)); g.draw(new Line2D.Double(13, 9, 6, 13));
                g.fill(new Ellipse2D.Double(2.5, 2.5, 5, 5)); g.fill(new Ellipse2D.Double(10.5, 6.5, 5, 5)); g.fill(new Ellipse2D.Double(3.5, 10.5, 5, 5));
            }
            case REMINDERS -> {
                g.draw(new Arc2D.Double(4, 3, 10, 12, 0, 180, Arc2D.OPEN));
                g.draw(new Line2D.Double(4, 9, 4, 13)); g.draw(new Line2D.Double(14, 9, 14, 13));
                g.draw(new Line2D.Double(2.5, 13, 15.5, 13)); g.draw(new Line2D.Double(8, 16, 10, 16));
            }
            case HISTORY -> {
                g.draw(new Ellipse2D.Double(2, 2, 14, 14));
                g.draw(new Line2D.Double(9, 5, 9, 9)); g.draw(new Line2D.Double(9, 9, 12, 11));
            }
        }
        g.dispose();
    }

    private static RoundRectangle2D rr(double x, double y, double w, double h) {
        return new RoundRectangle2D.Double(x, y, w, h, 3, 3);
    }
}
