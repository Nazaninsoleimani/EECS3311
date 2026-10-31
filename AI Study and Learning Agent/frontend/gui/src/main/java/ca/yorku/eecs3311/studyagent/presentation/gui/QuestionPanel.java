package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.GradeResult;
import ca.yorku.eecs3311.studyagent.domain.MultipleChoiceQuestion;
import ca.yorku.eecs3311.studyagent.domain.Question;
import ca.yorku.eecs3311.studyagent.domain.QuestionResponse;
import ca.yorku.eecs3311.studyagent.domain.QuestionType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;

/** One question with the input that fits its type: radio buttons for MCQ and true/false, a text box for short answers. */
public class QuestionPanel extends JPanel {
    private final Question question;
    private final List<JRadioButton> choices = new ArrayList<>();
    private final JTextArea text = new JTextArea(3, 50);
    private final JLabel feedback = new JLabel();
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

    public QuestionPanel(int number, Question q) {
        super(new BorderLayout(4, 4));
        this.question = q;
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 4, 8, 4)));
        String meta = q.getType().name().toLowerCase().replace('_', ' ') + ", " + q.getDifficulty().name().toLowerCase()
                + (q.getConceptName() == null ? "" : ", " + q.getConceptName());
        add(new JLabel("<html><b>Q" + number + ".</b> " + escape(q.getPrompt()) + " <i>(" + meta + ")</i></html>"), BorderLayout.NORTH);
        add(input(), BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(feedback, BorderLayout.CENTER);
        south.add(actions, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);
    }

    private JComponent input() {
        List<String> options = question instanceof MultipleChoiceQuestion mcq ? mcq.getOptions()
                : question.getType() == QuestionType.TRUE_FALSE ? List.of("True", "False") : null;
        if (options == null) {
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            return BaseView.scroll(text);
        }
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < options.size(); i++) {
            String label = question.getType() == QuestionType.TRUE_FALSE ? options.get(i) : (char) ('A' + i) + ")  " + options.get(i);
            JRadioButton b = new JRadioButton(label);
            group.add(b);
            choices.add(b);
            p.add(b);
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

    /** Locks the inputs and shows the grade in green or red. */
    public void showResult(GradeResult r) {
        choices.forEach(b -> b.setEnabled(false));
        text.setEditable(false);
        feedback.setForeground(r.correct ? new Color(0, 128, 0) : new Color(180, 0, 0));
        feedback.setText("<html>" + (r.correct ? "Correct. " : "Incorrect. ") + escape(r.feedback) + "</html>");
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
}
