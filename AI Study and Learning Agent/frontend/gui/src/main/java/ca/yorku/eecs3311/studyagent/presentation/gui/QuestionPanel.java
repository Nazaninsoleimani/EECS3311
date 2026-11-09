package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.GradeResult;
import ca.yorku.eecs3311.studyagent.domain.MultipleChoiceQuestion;
import ca.yorku.eecs3311.studyagent.domain.Question;
import ca.yorku.eecs3311.studyagent.domain.QuestionResponse;
import ca.yorku.eecs3311.studyagent.domain.QuestionType;
import ca.yorku.eecs3311.studyagent.domain.TrueFalseQuestion;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * One question as a card: a header with its number, type, difficulty and concept; the prompt (wrapped); clickable
 * option rows for MCQ and true/false or a text box for short answers; and, once graded, a coloured result banner
 * that marks the right and wrong options.
 */
public class QuestionPanel extends Card {
    private final Question question;
    private final List<JRadioButton> choices = new ArrayList<>();
    private final List<OptionRow> rows = new ArrayList<>();
    private final JTextArea text = new JTextArea(3, 40);
    private final JLabel feedback = new JLabel();
    private final JTextArea feedbackDetail = wrapping("", 13.5f, Font.PLAIN);
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    private final List<Runnable> answerListeners = new ArrayList<>();
    private boolean graded;

    public QuestionPanel(int number, Question q) {
        super(new BorderLayout(0, 12));
        this.question = q;
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(header(number), BorderLayout.NORTH);
        top.add(wrapping(q.getPrompt(), 15.5f, Font.BOLD), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(input(), BorderLayout.CENTER);
        actions.setOpaque(false);
    }

    private JPanel header(int number) {
        JPanel h = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        h.setOpaque(false);
        Card.Pill n = new Card.Pill("QUESTION " + number, Theme.ACCENT);
        n.setFont(n.getFont().deriveFont(Font.BOLD, 11.5f));
        h.add(n);
        h.add(chip(switch (question.getType()) {
            case MULTIPLE_CHOICE -> "Multiple choice";
            case TRUE_FALSE -> "True / false";
            case SHORT_ANSWER -> "Short answer";
        }));
        h.add(chip(capitalise(question.getDifficulty().name())));
        if (question.getConceptName() != null && !question.getConceptName().isBlank()) h.add(chip(question.getConceptName()));
        return h;
    }

    private static Card.Pill chip(String s) {
        Card.Pill p = new Card.Pill(s, Theme.muted());
        p.setFont(p.getFont().deriveFont(11.5f));
        return p;
    }

    private JComponent input() {
        List<String> options = question instanceof MultipleChoiceQuestion mcq ? mcq.getOptions()
                : question.getType() == QuestionType.TRUE_FALSE ? List.of("True", "False") : null;
        if (options == null) {
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setFont(text.getFont().deriveFont(14f));
            text.putClientProperty("JTextField.placeholderText", "Type your answer in your own words...");
            text.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            text.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { fireAnswerChanged(); }
                @Override public void removeUpdate(DocumentEvent e) { fireAnswerChanged(); }
                @Override public void changedUpdate(DocumentEvent e) { fireAnswerChanged(); }
            });
            JScrollPane sp = new JScrollPane(text);
            sp.setBorder(BorderFactory.createLineBorder(Theme.border()));
            return sp;
        }
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 8));
        p.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < options.size(); i++) {
            String letter = question.getType() == QuestionType.TRUE_FALSE ? null : String.valueOf((char) ('A' + i));
            OptionRow row = new OptionRow(letter, options.get(i));
            group.add(row.radio);
            choices.add(row.radio);
            rows.add(row);
            p.add(row);
        }
        return p;
    }

    /** "A".."E" for MCQ, "true"/"false", or the typed text; empty if unanswered. */
    public String answer() {
        if (choices.isEmpty()) return text.getText().trim();
        for (int i = 0; i < choices.size(); i++) {
            if (!choices.get(i).isSelected()) continue;
            return question.getType() == QuestionType.TRUE_FALSE ? (i == 0 ? "true" : "false") : String.valueOf((char) ('A' + i));
        }
        return "";
    }

    public QuestionResponse response() {
        return new QuestionResponse(question.getId(), answer());
    }

    /** Runs whenever the student picks an option or edits the text (used for the progress bar). */
    public void onAnswerChanged(Runnable r) { answerListeners.add(r); }

    private void fireAnswerChanged() { answerListeners.forEach(Runnable::run); }

    /** Locks the inputs, marks the right (and the chosen wrong) option, and shows the grade in a coloured banner. */
    public void showResult(GradeResult r) {
        graded = true;
        choices.forEach(b -> b.setEnabled(false));
        text.setEditable(false);
        int correct = question instanceof MultipleChoiceQuestion mcq ? mcq.getCorrectIndex()
                : question instanceof TrueFalseQuestion tf ? (tf.getCorrectValue() ? 0 : 1) : -1;
        for (int i = 0; i < rows.size(); i++) {
            OptionRow row = rows.get(i);
            if (i == correct) row.mark(Theme.SUCCESS, row.radio.isSelected() ? "✓ Your answer" : "✓ Correct answer");
            else if (row.radio.isSelected()) row.mark(Theme.DANGER, "✗ Your answer");
            else row.mark(null, null);
        }

        Color tone = r.correct ? Theme.SUCCESS : Theme.DANGER;
        feedback.setText(r.correct ? "✓  Correct." : "✗  Incorrect.");
        feedback.setForeground(tone);
        feedback.setFont(feedback.getFont().deriveFont(Font.BOLD, 15f));
        // The grader's text often starts with "Correct." / "Incorrect." too; do not say it twice.
        String detail = r.feedback == null ? "" : r.feedback.replaceFirst("^\\s*(Correct|Incorrect)\\.\\s*", "").trim();
        if (detail.isEmpty()) detail = r.correct ? "Nice work - keep it up!" : "";
        feedbackDetail.setText(detail);

        Card banner = new Card(new BorderLayout(12, 4), tone);
        banner.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JPanel textCol = new JPanel(new BorderLayout(0, 4));
        textCol.setOpaque(false);
        textCol.add(feedback, BorderLayout.NORTH);
        if (!detail.isEmpty()) textCol.add(feedbackDetail, BorderLayout.CENTER);
        banner.add(textCol, BorderLayout.CENTER);
        banner.add(actions, BorderLayout.EAST);
        add(banner, BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    public void addAction(JComponent c) {
        actions.add(c);
        revalidate();
    }

    public Question question() { return question; }

    /** For tests and keyboard-free demos: selects option i (0-based) or types the text. */
    void answerWith(String value) {
        if (choices.isEmpty()) text.setText(value);
        else choices.get(Integer.parseInt(value)).setSelected(true);
    }

    JLabel feedbackLabel() { return feedback; }

    static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : s.charAt(0) + s.substring(1).toLowerCase();
    }

    /** Read-only, word-wrapping text that looks like a label. */
    static JTextArea wrapping(String s, float size, int style) {
        JTextArea a = new JTextArea(s);
        a.setEditable(false);
        a.setFocusable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setOpaque(false);
        a.setBorder(null);
        a.setFont(new JLabel().getFont().deriveFont(style, size));
        return a;
    }

    /** One answer option: a rounded, clickable row that highlights on hover and selection, and shows the grade. */
    private final class OptionRow extends JPanel {
        final JRadioButton radio = new JRadioButton();
        private final JLabel tag = new JLabel();
        private Color mark;
        private boolean hover;

        OptionRow(String letter, String label) {
            super(new BorderLayout(10, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 14));
            radio.setOpaque(false);
            radio.addItemListener(e -> {
                rows.forEach(JComponent::repaint);
                fireAnswerChanged();
            });
            JPanel west = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            west.setOpaque(false);
            west.add(radio);
            if (letter != null) {
                JLabel l = new JLabel(letter);
                l.setFont(l.getFont().deriveFont(Font.BOLD, 14.5f));
                l.setForeground(Theme.ACCENT);
                west.add(l);
            }
            add(west, BorderLayout.WEST);
            JTextArea body = wrapping(label, 14f, Font.PLAIN);
            add(body, BorderLayout.CENTER);
            tag.setFont(tag.getFont().deriveFont(Font.BOLD, 12f));
            add(tag, BorderLayout.EAST);

            MouseAdapter click = new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { if (!graded) radio.setSelected(true); }
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            };
            for (JComponent c : new JComponent[]{this, body, west}) {
                c.addMouseListener(click);
                c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }

        void mark(Color c, String label) {
            mark = c;
            tag.setText(label == null ? "" : label);
            if (c != null) tag.setForeground(c);
            setCursor(Cursor.getDefaultCursor());
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill, line;
            if (mark != null) {
                fill = Theme.tint(mark, 0.14);
                line = mark;
            } else if (radio.isSelected() && !graded) {
                fill = Theme.tint(Theme.ACCENT, 0.12);
                line = Theme.ACCENT;
            } else {
                fill = hover && !graded ? Theme.tint(Theme.ACCENT, 0.05) : Theme.card();
                line = hover && !graded ? Theme.tint(Theme.ACCENT, 0.5) : Theme.border();
            }
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g.setColor(line);
            g.setStroke(new BasicStroke(mark != null || (radio.isSelected() && !graded) ? 1.6f : 1f));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g.dispose();
            super.paintComponent(g0);
        }
    }
}
