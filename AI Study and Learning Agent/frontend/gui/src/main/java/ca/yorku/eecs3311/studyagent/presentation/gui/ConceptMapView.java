package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Concept;
import ca.yorku.eecs3311.studyagent.domain.ConceptGraph;
import ca.yorku.eecs3311.studyagent.domain.ConceptRelation;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.RelationType;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

/** F15: the course's concepts drawn as a graph, coloured by the student's mastery; click a concept for details. */
public class ConceptMapView extends BaseView {
    static final Color UNPRACTISED = new Color(220, 220, 220);
    static final Color WEAK = new Color(239, 154, 154);
    static final Color LEARNING = new Color(255, 213, 128);
    static final Color MASTERED = new Color(165, 214, 167);

    private final Canvas canvas = new Canvas();
    private final JTextArea details = readOnlyArea();
    private ConceptGraph graph = new ConceptGraph();
    private String selectedId;

    public ConceptMapView(GuiContext ctx) {
        super(ctx);
        details.setColumns(28);
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading("Concept map"), BorderLayout.NORTH);
        top.add(row(legend("not practised", UNPRACTISED), legend("weak (<50%)", WEAK), legend("learning", LEARNING),
                legend("mastered (≥80%)", MASTERED), button("Copy as Mermaid", this::copyMermaid)), BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, scroll(details));
        split.setResizeWeight(0.75);
        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    private static JLabel legend(String text, Color c) {
        JLabel l = new JLabel(" " + text + " ");
        l.setOpaque(true);
        l.setBackground(c);
        return l;
    }

    @Override public String title() { return "Concept Map"; }

    @Override
    public void render() {
        Course c = ctx.course();
        if (c == null) {
            graph = new ConceptGraph();
            details.setText("Select a course first.");
        } else {
            loadConceptMap(c.getId());
        }
        canvas.repaint();
    }

    public void loadConceptMap(String courseId) {
        graph = facade.getConceptMap(ctx.studentId, courseId);
        selectedId = null;
        details.setText(graph.isEmpty() ? "No concepts yet. Use Extract concepts on a material (Materials tab)."
                : graph.nodes.size() + " concepts, " + graph.edges.size() + " relations.\nClick a concept for its details.");
        canvas.repaint();
    }

    public void selectConcept(String conceptId) {
        selectedId = conceptId;
        graph.find(conceptId).ifPresent(c -> details.setText(describe(graph, c)));
        canvas.repaint();
    }

    static String describe(ConceptGraph g, Concept c) {
        Double m = g.mastery.get(c.getId());
        StringBuilder sb = new StringBuilder(c.getName()).append("\n\n");
        if (!c.getDefinition().isBlank()) sb.append(c.getDefinition()).append("\n\n");
        sb.append("Mastery: ").append(m == null ? "not practised yet" : Math.round(m * 100) + "%").append('\n');
        List<Concept> prereqs = g.prerequisitesOf(c.getId());
        if (!prereqs.isEmpty()) sb.append("Learn first: ").append(names(prereqs)).append('\n');
        List<Concept> related = g.neighbours(c.getId());
        related.removeAll(prereqs);
        if (!related.isEmpty()) sb.append("Connected to: ").append(names(related)).append('\n');
        return sb.toString();
    }

    private static String names(List<Concept> list) {
        return list.stream().map(Concept::getName).collect(Collectors.joining(", "));
    }

    private void copyMermaid() {
        if (graph.isEmpty()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(graph.toMermaid()), null);
        ctx.status("Mermaid diagram copied to the clipboard");
    }

    static Color colorFor(Double mastery) {
        if (mastery == null) return UNPRACTISED;
        if (mastery < 0.5) return WEAK;
        if (mastery < 0.8) return LEARNING;
        return MASTERED;
    }

    /** Places concepts on a circle (prerequisites first) and draws relations as arrows. */
    private final class Canvas extends JComponent {
        private final Map<String, Point> positions = new HashMap<>();

        Canvas() {
            setPreferredSize(new Dimension(700, 500));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    positions.entrySet().stream().filter(p -> p.getValue().distance(e.getPoint()) < 45)
                            .min((a, b) -> Double.compare(a.getValue().distance(e.getPoint()), b.getValue().distance(e.getPoint())))
                            .ifPresent(p -> selectConcept(p.getKey()));
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            positions.clear();
            List<Concept> nodes = graph.nodes.stream()
                    .sorted((a, b) -> Integer.compare(graph.dependentsCount(b.getId()), graph.dependentsCount(a.getId()))).toList();
            int n = nodes.size();
            if (n == 0) return;
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int radius = n == 1 ? 0 : Math.min(cx, cy) - 70;
            for (int i = 0; i < n; i++) {
                double angle = 2 * Math.PI * i / n - Math.PI / 2;
                positions.put(nodes.get(i).getId(), new Point(cx + (int) (radius * Math.cos(angle)), cy + (int) (radius * Math.sin(angle))));
            }
            for (ConceptRelation r : graph.edges) {
                Point a = positions.get(r.getFromId());
                Point b = positions.get(r.getToId());
                if (a == null || b == null) continue;
                g.setColor(r.getType() == RelationType.PREREQUISITE_OF ? new Color(60, 90, 160) : Color.GRAY);
                g.setStroke(r.getType() == RelationType.RELATED_TO
                        ? new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 4}, 0) : new BasicStroke(1.6f));
                g.draw(new Line2D.Double(a, b));
                arrowHead(g, a, b);
                g.drawString(r.getType().name().toLowerCase().replace('_', ' '), (a.x + b.x) / 2 + 4, (a.y + b.y) / 2 - 4);
            }
            g.setStroke(new BasicStroke(1.5f));
            FontMetrics fm = g.getFontMetrics();
            for (Concept c : nodes) {
                Point p = positions.get(c.getId());
                Double m = graph.mastery.get(c.getId());
                String label = c.getName() + (m == null ? "" : " " + Math.round(m * 100) + "%");
                int w = Math.min(180, fm.stringWidth(label) + 16);
                int h = fm.getHeight() + 10;
                g.setColor(colorFor(m));
                g.fillRoundRect(p.x - w / 2, p.y - h / 2, w, h, 14, 14);
                g.setColor(c.getId().equals(selectedId) ? Color.BLUE : Color.DARK_GRAY);
                g.drawRoundRect(p.x - w / 2, p.y - h / 2, w, h, 14, 14);
                g.setColor(Color.BLACK);
                g.drawString(clip(label, fm, w - 10), p.x - w / 2 + 8, p.y + fm.getAscent() / 2 - 1);
            }
        }

        private void arrowHead(Graphics2D g, Point from, Point to) {
            double angle = Math.atan2(to.y - from.y, to.x - from.x);
            int tipX = (int) (to.x - 30 * Math.cos(angle));
            int tipY = (int) (to.y - 30 * Math.sin(angle));
            for (double side : new double[]{-0.45, 0.45}) {
                g.drawLine(tipX, tipY, (int) (tipX - 10 * Math.cos(angle + side)), (int) (tipY - 10 * Math.sin(angle + side)));
            }
        }

        private String clip(String s, FontMetrics fm, int width) {
            if (fm.stringWidth(s) <= width) return s;
            while (s.length() > 3 && fm.stringWidth(s + "…") > width) s = s.substring(0, s.length() - 1);
            return s + "…";
        }
    }

    ConceptGraph graph() { return graph; }
    JTextArea detailsArea() { return details; }
}
