package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Concept;
import ca.yorku.eecs3311.studyagent.domain.ConceptGraph;
import ca.yorku.eecs3311.studyagent.domain.ConceptRelation;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.RelationType;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * F15: the course's concepts drawn as a layered graph, coloured by the student's mastery. Prerequisites sit to the
 * left of what they unlock; hovering or clicking a concept highlights its connections and shows its details.
 */
public class ConceptMapView extends BaseView {
    static final Color UNPRACTISED = new Color(220, 220, 220);
    static final Color WEAK = new Color(239, 154, 154);
    static final Color LEARNING = new Color(255, 213, 128);
    static final Color MASTERED = new Color(165, 214, 167);
    private static final Color NODE_TEXT = new Color(0x1B1F24);

    private final Canvas canvas = new Canvas();
    private final JTextArea details = readOnlyArea();
    private ConceptGraph graph = new ConceptGraph();
    private String selectedId;
    private final JButton quizMe = button("Quiz me on this", this::quizOnSelected);

    public ConceptMapView(GuiContext ctx) {
        super(ctx);
        details.setColumns(28);
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading("Concept map"), BorderLayout.NORTH);
        JPanel legends = new JPanel(new BorderLayout());
        legends.setOpaque(false);
        legends.add(row(legend("not practised", UNPRACTISED), legend("weak (<50%)", WEAK), legend("learning", LEARNING),
                legend("mastered (≥80%)", MASTERED), edgeLegend("prerequisite of", RelationType.PREREQUISITE_OF),
                edgeLegend("part of", RelationType.PART_OF), edgeLegend("related to", RelationType.RELATED_TO)), BorderLayout.NORTH);
        legends.add(row(button("Copy as Mermaid", this::copyMermaid), quizMe), BorderLayout.SOUTH);
        top.add(legends, BorderLayout.SOUTH);
        quizMe.setEnabled(false);
        JScrollPane canvasScroll = new JScrollPane(canvas);
        canvasScroll.setBorder(BorderFactory.createLineBorder(Theme.border()));
        canvasScroll.getVerticalScrollBar().setUnitIncrement(16);
        canvasScroll.getHorizontalScrollBar().setUnitIncrement(16);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvasScroll, scroll(details));
        split.setResizeWeight(0.75);
        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    private static JLabel legend(String text, Color c) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(c);
        l.setForeground(NODE_TEXT);
        l.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        return l;
    }

    private static JLabel edgeLegend(String text, RelationType type) {
        JLabel l = new JLabel(text, new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(edgeColor(type));
                g.setStroke(edgeStroke(type, false));
                int mid = y + getIconHeight() / 2;
                g.drawLine(x + 2, mid, x + 26, mid);
                if (type != RelationType.RELATED_TO) g.fill(arrowHead(new Point2D.Double(x + 18, mid), new Point2D.Double(x + 30, mid)));
                g.dispose();
            }

            @Override public int getIconWidth() { return 32; }
            @Override public int getIconHeight() { return 12; }
        }, SwingConstants.LEFT);
        l.setIconTextGap(4);
        return l;
    }

    @Override public String title() { return "Concept Map"; }

    @Override
    public void render() {
        Course c = ctx.course();
        if (c == null) {
            graph = new ConceptGraph();
            selectedId = null;
            details.setText("Select a course first.");
            canvas.relayout();
        } else {
            loadConceptMap(c.getId());
        }
    }

    public void loadConceptMap(String courseId) {
        graph = facade.getConceptMap(ctx.studentId, courseId);
        selectedId = null;
        quizMe.setEnabled(false);
        details.setText(overview());
        canvas.relayout();
    }

    private String overview() {
        return graph.isEmpty() ? "No concepts yet. Use Extract concepts on a material (Materials tab)."
                : graph.nodes.size() + " concepts, " + graph.edges.size() + " relations.\n\n"
                + "Prerequisites are on the left, the concepts they lead to on the right.\n"
                + "Hover over a concept to see its connections; click it for details.";
    }

    public void selectConcept(String conceptId) {
        selectedId = conceptId;
        quizMe.setEnabled(graph.find(conceptId).isPresent());
        graph.find(conceptId).ifPresent(c -> details.setText(describe(graph, c)));
        canvas.repaint();
    }

    private void clearSelection() {
        selectedId = null;
        quizMe.setEnabled(false);
        details.setText(overview());
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

    private void quizOnSelected() {
        if (selectedId != null) graph.find(selectedId).ifPresent(c -> ctx.requestQuizOn(c.getName()));
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

    static Color edgeColor(RelationType type) {
        return switch (type) {
            case PREREQUISITE_OF -> Theme.ACCENT;
            case PART_OF -> Theme.withAlpha(Theme.text(), 170);
            case RELATED_TO -> Theme.muted();
        };
    }

    static Stroke edgeStroke(RelationType type, boolean emphasised) {
        float w = emphasised ? 2.6f : 1.6f;
        return type == RelationType.RELATED_TO
                ? new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[]{6, 5}, 0)
                : new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    /** Filled triangle whose tip is at {@code tip}, pointing away from {@code from}. */
    static Path2D arrowHead(Point2D from, Point2D tip) {
        double angle = Math.atan2(tip.getY() - from.getY(), tip.getX() - from.getX());
        Path2D p = new Path2D.Double();
        p.moveTo(tip.getX(), tip.getY());
        p.lineTo(tip.getX() - 11 * Math.cos(angle - 0.4), tip.getY() - 11 * Math.sin(angle - 0.4));
        p.lineTo(tip.getX() - 11 * Math.cos(angle + 0.4), tip.getY() - 11 * Math.sin(angle + 0.4));
        p.closePath();
        return p;
    }

    // ------------------------------------------------------------------ layout

    static final int NODE_W = 180;
    static final int NODE_H = 76;
    static final int COL_GAP = 90;
    static final int ROW_GAP = 26;
    static final int PAD = 30;
    static final int MAX_PER_COLUMN = 7;

    /**
     * Layered layout: a concept's column is the length of the longest prerequisite / part-of chain leading to it.
     * Graphs with only "related to" links are layered by distance from their best-connected concept instead.
     * Within each column concepts are ordered next to their neighbours (barycenter sweeps) to reduce crossings;
     * concepts without any relation are placed in a grid below the graph.
     */
    static Map<String, Rectangle> computeLayout(ConceptGraph g) {
        Map<String, Rectangle> boxes = new LinkedHashMap<>();
        if (g.isEmpty()) return boxes;
        List<String> ids = g.nodes.stream().map(Concept::getId).toList();
        Set<String> known = new HashSet<>(ids);
        Map<String, Set<String>> adj = new HashMap<>();
        ids.forEach(id -> adj.put(id, new HashSet<>()));
        List<String[]> hierarchy = new ArrayList<>();   // {earlier column, later column}
        for (ConceptRelation r : g.edges) {
            String a = r.getFromId(), b = r.getToId();
            if (!known.contains(a) || !known.contains(b) || a.equals(b)) continue;
            adj.get(a).add(b);
            adj.get(b).add(a);
            if (r.getType() == RelationType.PREREQUISITE_OF) hierarchy.add(new String[]{a, b});
            else if (r.getType() == RelationType.PART_OF) hierarchy.add(new String[]{b, a});   // whole before its parts
        }
        List<String> linked = ids.stream().filter(id -> !adj.get(id).isEmpty()).toList();
        List<String> isolated = ids.stream().filter(id -> adj.get(id).isEmpty()).toList();

        Map<String, Integer> level = new HashMap<>();
        linked.forEach(id -> level.put(id, 0));
        for (int pass = 0; pass < linked.size(); pass++) {   // longest path; bounded so cycles cannot loop forever
            boolean changed = false;
            for (String[] e : hierarchy) {
                if (level.get(e[1]) < level.get(e[0]) + 1) {
                    level.put(e[1], level.get(e[0]) + 1);
                    changed = true;
                }
            }
            if (!changed) break;
        }
        if (!linked.isEmpty() && level.values().stream().allMatch(l -> l == 0)) bfsLevels(linked, adj, level);

        // Group into columns, splitting very tall columns so the map stays readable.
        Map<Integer, List<String>> byLevel = new HashMap<>();
        for (String id : linked) byLevel.computeIfAbsent(level.get(id), k -> new ArrayList<>()).add(id);
        List<List<String>> columns = new ArrayList<>();
        byLevel.keySet().stream().sorted().forEach(l -> {
            List<String> col = byLevel.get(l);
            for (int i = 0; i < col.size(); i += MAX_PER_COLUMN) {
                columns.add(new ArrayList<>(col.subList(i, Math.min(col.size(), i + MAX_PER_COLUMN))));
            }
        });

        Map<String, Double> rank = new HashMap<>();
        Runnable updateRanks = () -> columns.forEach(col -> {
            for (int i = 0; i < col.size(); i++) rank.put(col.get(i), i - (col.size() - 1) / 2.0);
        });
        updateRanks.run();
        for (int sweep = 0; sweep < 8; sweep++) {
            boolean forward = sweep % 2 == 0;
            for (int ci = 0; ci < columns.size(); ci++) {
                List<String> col = columns.get(forward ? ci : columns.size() - 1 - ci);
                Map<String, Double> bary = new HashMap<>();
                for (String id : col) {
                    double sum = 0;
                    int n = 0;
                    for (String nb : adj.get(id)) {
                        if (col.contains(nb)) continue;
                        sum += rank.get(nb);
                        n++;
                    }
                    bary.put(id, n == 0 ? rank.get(id) : sum / n);
                }
                col.sort(Comparator.comparingDouble(bary::get));
                for (int i = 0; i < col.size(); i++) rank.put(col.get(i), i - (col.size() - 1) / 2.0);
            }
        }

        int tallest = columns.stream().mapToInt(List::size).max().orElse(0);
        int graphH = tallest == 0 ? 0 : tallest * NODE_H + (tallest - 1) * ROW_GAP;
        for (int ci = 0; ci < columns.size(); ci++) {
            List<String> col = columns.get(ci);
            int colH = col.size() * NODE_H + (col.size() - 1) * ROW_GAP;
            int y0 = PAD + (graphH - colH) / 2;
            for (int i = 0; i < col.size(); i++) {
                boxes.put(col.get(i), new Rectangle(PAD + ci * (NODE_W + COL_GAP), y0 + i * (NODE_H + ROW_GAP), NODE_W, NODE_H));
            }
        }

        if (!isolated.isEmpty()) {
            int perRow = Math.max(4, columns.size());
            int top = linked.isEmpty() ? PAD : PAD + graphH + 3 * ROW_GAP + 18;   // room for the "not linked" caption
            for (int i = 0; i < isolated.size(); i++) {
                boxes.put(isolated.get(i), new Rectangle(PAD + (i % perRow) * (NODE_W + ROW_GAP),
                        top + (i / perRow) * (NODE_H + ROW_GAP), NODE_W, NODE_H));
            }
        }
        return boxes;
    }

    private static void bfsLevels(List<String> linked, Map<String, Set<String>> adj, Map<String, Integer> level) {
        Set<String> seen = new HashSet<>();
        List<String> byDegree = new ArrayList<>(linked);
        byDegree.sort(Comparator.comparingInt((String id) -> adj.get(id).size()).reversed());
        for (String root : byDegree) {
            if (seen.contains(root)) continue;
            Deque<String> queue = new ArrayDeque<>(List.of(root));
            seen.add(root);
            level.put(root, 0);
            while (!queue.isEmpty()) {
                String cur = queue.poll();
                for (String nb : adj.get(cur)) {
                    if (seen.add(nb)) {
                        level.put(nb, level.get(cur) + 1);
                        queue.add(nb);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    private final class Canvas extends JComponent implements Scrollable {
        private Map<String, Rectangle> boxes = new LinkedHashMap<>();
        private Dimension content = new Dimension(0, 0);
        private String hoveredId;

        Canvas() {
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    String id = nodeAt(e.getPoint());
                    if (id != null) selectConcept(id);
                    else if (selectedId != null) clearSelection();
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    String id = nodeAt(e.getPoint());
                    if (!java.util.Objects.equals(id, hoveredId)) {
                        hoveredId = id;
                        setCursor(Cursor.getPredefinedCursor(id == null ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
                        setToolTipText(id == null ? null : graph.find(id).map(Concept::getName).orElse(null));
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (hoveredId != null) {
                        hoveredId = null;
                        repaint();
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        void relayout() {
            boxes = computeLayout(graph);
            int w = 0, h = 0;
            for (Rectangle r : boxes.values()) {
                w = Math.max(w, r.x + r.width);
                h = Math.max(h, r.y + r.height);
            }
            content = new Dimension(w + PAD + 70, h + PAD);   // extra room on the right for same-column curves
            hoveredId = null;
            revalidate();
            repaint();
        }

        /** Shift that centres the map when the viewport is larger than it. */
        private Point offset() {
            return new Point(Math.max(0, (getWidth() - content.width) / 2), Math.max(0, (getHeight() - content.height) / 2));
        }

        private Rectangle box(String id) {
            Rectangle r = boxes.get(id);
            if (r == null) return null;
            Point o = offset();
            return new Rectangle(r.x + o.x, r.y + o.y, r.width, r.height);
        }

        private String nodeAt(Point p) {
            for (String id : boxes.keySet()) {
                if (box(id).contains(p)) return id;
            }
            return null;
        }

        @Override public Dimension getPreferredSize() { return new Dimension(Math.max(content.width, 500), Math.max(content.height, 360)); }
        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(700, 500); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 160; }
        @Override public boolean getScrollableTracksViewportWidth() { return getParent() != null && getParent().getWidth() > getPreferredSize().width; }
        @Override public boolean getScrollableTracksViewportHeight() { return getParent() != null && getParent().getHeight() > getPreferredSize().height; }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setColor(Theme.card());
            g.fillRect(0, 0, getWidth(), getHeight());
            if (boxes.isEmpty()) {
                g.setColor(Theme.muted());
                String msg = graph.isEmpty() ? "No concepts yet - use Extract concepts on a material." : "";
                g.drawString(msg, (getWidth() - g.getFontMetrics().stringWidth(msg)) / 2, getHeight() / 2);
                g.dispose();
                return;
            }

            String focus = hoveredId != null ? hoveredId : selectedId;
            Set<String> neighbourhood = new HashSet<>();
            if (focus != null) {
                neighbourhood.add(focus);
                graph.neighbours(focus).forEach(c -> neighbourhood.add(c.getId()));
            }

            List<ConceptRelation> dim = new ArrayList<>();
            List<ConceptRelation> lit = new ArrayList<>();
            for (ConceptRelation r : graph.edges) {
                boolean touches = focus != null && (r.getFromId().equals(focus) || r.getToId().equals(focus));
                (touches ? lit : dim).add(r);
            }
            for (ConceptRelation r : dim) drawEdge(g, r, focus == null ? 1f : 0.15f, false);
            for (ConceptRelation r : lit) drawEdge(g, r, 1f, true);

            drawUnlinkedCaption(g);
            for (Concept c : graph.nodes) {
                Rectangle b = box(c.getId());
                if (b == null) continue;
                boolean faded = focus != null && !neighbourhood.contains(c.getId());
                drawNode(g, c, b, faded);
            }
            g.dispose();
        }

        private void drawUnlinkedCaption(Graphics2D g) {
            Rectangle firstIsolated = null;
            boolean anyLinked = false;
            for (Concept c : graph.nodes) {
                boolean linked = graph.edges.stream().anyMatch(r -> !r.getFromId().equals(r.getToId())
                        && (r.getFromId().equals(c.getId()) || r.getToId().equals(c.getId())));
                if (linked) anyLinked = true;
                else if (firstIsolated == null) firstIsolated = box(c.getId());
            }
            if (firstIsolated == null || !anyLinked) return;
            g.setColor(Theme.muted());
            g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
            g.drawString("Not linked to other concepts yet", firstIsolated.x, firstIsolated.y - 10);
        }

        private void drawEdge(Graphics2D g, ConceptRelation r, float alpha, boolean emphasised) {
            Rectangle a = box(r.getFromId());
            Rectangle b = box(r.getToId());
            if (a == null || b == null || a.equals(b)) return;
            // "related to" has no direction: always draw it left to right so it never doubles back.
            if (r.getType() == RelationType.RELATED_TO && a.x > b.x) {
                Rectangle t = a;
                a = b;
                b = t;
            }
            Point2D start, end, c1, c2;
            if (Math.abs(a.x - b.x) < NODE_W / 2) {
                // Same column: bow out to the right side.
                start = new Point2D.Double(a.getMaxX(), a.getCenterY());
                end = new Point2D.Double(b.getMaxX(), b.getCenterY());
                double bow = 30 + Math.abs(a.getCenterY() - b.getCenterY()) * 0.25;
                c1 = new Point2D.Double(start.getX() + bow, start.getY());
                c2 = new Point2D.Double(end.getX() + bow, end.getY());
            } else {
                boolean rightward = b.x > a.x;
                start = new Point2D.Double(rightward ? a.getMaxX() : a.getMinX(), a.getCenterY());
                end = new Point2D.Double(rightward ? b.getMinX() : b.getMaxX(), b.getCenterY());
                double dx = (end.getX() - start.getX()) * 0.5;
                c1 = new Point2D.Double(start.getX() + dx, start.getY());
                c2 = new Point2D.Double(end.getX() - dx, end.getY());
            }
            Graphics2D e = (Graphics2D) g.create();
            e.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            e.setColor(edgeColor(r.getType()));
            e.setStroke(edgeStroke(r.getType(), emphasised));
            e.draw(new CubicCurve2D.Double(start.getX(), start.getY(), c1.getX(), c1.getY(), c2.getX(), c2.getY(), end.getX(), end.getY()));
            if (r.getType() != RelationType.RELATED_TO) e.fill(arrowHead(c2, end));
            e.dispose();
        }

        private void drawNode(Graphics2D g, Concept c, Rectangle b, boolean faded) {
            Graphics2D n = (Graphics2D) g.create();
            if (faded) n.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            Double m = graph.mastery.get(c.getId());
            boolean selected = c.getId().equals(selectedId);
            boolean hovered = c.getId().equals(hoveredId);
            RoundRectangle2D shape = new RoundRectangle2D.Double(b.x, b.y, b.width, b.height, 16, 16);

            n.setColor(Theme.withAlpha(Color.BLACK, 28));
            n.fill(new RoundRectangle2D.Double(b.x + 1, b.y + 3, b.width, b.height, 16, 16));
            n.setColor(colorFor(m));
            n.fill(shape);

            // Mastery bar along the bottom of the card.
            int barX = b.x + 12, barW = b.width - 24, barY = b.y + b.height - 12;
            n.setColor(Theme.withAlpha(NODE_TEXT, 30));
            n.fillRoundRect(barX, barY, barW, 5, 5, 5);
            if (m != null) {
                n.setColor(Theme.withAlpha(NODE_TEXT, 150));
                n.fillRoundRect(barX, barY, (int) Math.round(barW * Math.max(0, Math.min(1, m))), 5, 5, 5);
            }

            n.setStroke(new BasicStroke(selected ? 3f : hovered ? 2f : 1f));
            n.setColor(selected || hovered ? Theme.ACCENT : Theme.withAlpha(NODE_TEXT, 60));
            n.draw(shape);

            Font base = n.getFont();
            n.setFont(base.deriveFont(Font.BOLD, 12.5f));
            FontMetrics fm = n.getFontMetrics();
            List<String> lines = wrap(c.getName(), fm, b.width - 20, 2);
            n.setColor(NODE_TEXT);
            int y = b.y + 10 + fm.getAscent();
            for (String line : lines) {
                n.drawString(line, b.x + (b.width - fm.stringWidth(line)) / 2, y);
                y += fm.getHeight() - 1;
            }
            n.setFont(base.deriveFont(Font.PLAIN, 10.5f));
            FontMetrics small = n.getFontMetrics();
            String sub = m == null ? "not practised" : Math.round(m * 100) + "% mastered";
            n.setColor(Theme.withAlpha(NODE_TEXT, 170));
            n.drawString(sub, b.x + (b.width - small.stringWidth(sub)) / 2, barY - 4);
            n.dispose();
        }

        /** Word-wraps text into at most maxLines lines, ending with an ellipsis if it does not fit. */
        private List<String> wrap(String text, FontMetrics fm, int width, int maxLines) {
            List<String> lines = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            String[] words = text.trim().split("\\s+");
            for (int i = 0; i < words.length; i++) {
                String candidate = cur.isEmpty() ? words[i] : cur + " " + words[i];
                if (fm.stringWidth(candidate) <= width || cur.isEmpty()) {
                    cur = new StringBuilder(candidate);
                } else {
                    lines.add(cur.toString());
                    cur = new StringBuilder(words[i]);
                    if (lines.size() == maxLines - 1) {
                        for (int j = i + 1; j < words.length; j++) cur.append(' ').append(words[j]);
                        break;
                    }
                }
            }
            if (!cur.isEmpty()) lines.add(cur.toString());
            for (int i = 0; i < lines.size(); i++) lines.set(i, clip(lines.get(i), fm, width));
            return lines;
        }

        private String clip(String s, FontMetrics fm, int width) {
            if (fm.stringWidth(s) <= width) return s;
            while (s.length() > 1 && fm.stringWidth(s + "…") > width) s = s.substring(0, s.length() - 1);
            return s.stripTrailing() + "…";
        }
    }

    ConceptGraph graph() { return graph; }
    JTextArea detailsArea() { return details; }
    JButton quizMeButton() { return quizMe; }
}
