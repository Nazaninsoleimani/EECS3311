package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.DifficultyLevel;
import ca.yorku.eecs3311.studyagent.domain.Explanation;
import ca.yorku.eecs3311.studyagent.domain.QuestionResponse;
import ca.yorku.eecs3311.studyagent.domain.QuestionType;
import ca.yorku.eecs3311.studyagent.domain.Quiz;
import ca.yorku.eecs3311.studyagent.domain.QuizAttempt;
import ca.yorku.eecs3311.studyagent.domain.QuizRequest;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/** F08 generate a quiz, F09 answer and grade it, F10 explain any graded answer. */
public class QuizView extends BaseView {
    private final JSpinner count = new JSpinner(new SpinnerNumberModel(5, 1, 30, 1));
    private final JComboBox<DifficultyLevel> difficulty = new JComboBox<>(DifficultyLevel.values());
    private final JCheckBox mcq = new JCheckBox("Multiple choice", true);
    private final JCheckBox tf = new JCheckBox("True/false", true);
    private final JCheckBox shortAnswer = new JCheckBox("Short answer", true);
    private final JTextField concepts = new JTextField(18);
    private final JPanel questions = new WidthTrackingPanel();
    private final JProgressBar answered = new JProgressBar();
    private final JLabel result = new JLabel(" ");
    private final JButton submit = accentButton("Submit answers", this::submitAnswers);
    private final List<QuestionPanel> panels = new ArrayList<>();
    private Quiz quiz;
    private QuizAttempt attempt;

    public QuizView(GuiContext ctx) {
        super(ctx);
        difficulty.setSelectedItem(DifficultyLevel.INTERMEDIATE);
        questions.setLayout(new BoxLayout(questions, BoxLayout.Y_AXIS));
        questions.setBorder(BorderFactory.createEmptyBorder(4, 2, 12, 8));
        concepts.putClientProperty("JTextField.placeholderText", "e.g. Polymorphism, Generics");

        // Settings in two rows so nothing is pushed off-screen.
        JPanel settings = new JPanel(new BorderLayout(0, 4));
        settings.setOpaque(false);
        JPanel first = row(new JLabel("Questions"), count, new JLabel("   Difficulty"), difficulty,
                new JLabel("   Concepts (optional)"), concepts);
        JPanel second = row(new JLabel("Question types"), mcq, tf, shortAnswer, new JLabel("   "),
                accentButton("Generate quiz", this::requestQuiz));
        for (JPanel r : List.of(first, second)) r.setOpaque(false);
        for (JCheckBox b : List.of(mcq, tf, shortAnswer)) b.setOpaque(false);
        settings.add(first, BorderLayout.NORTH);
        settings.add(second, BorderLayout.SOUTH);
        Card setup = new Card(new BorderLayout());
        setup.add(settings, BorderLayout.CENTER);
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.add(heading("Practice quiz"), BorderLayout.NORTH);
        top.add(setup, BorderLayout.CENTER);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JScrollPane sp = new JScrollPane(questions);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        answered.setStringPainted(true);
        answered.setPreferredSize(new Dimension(220, 18));
        answered.setVisible(false);
        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        bottom.add(row(submit, answered), BorderLayout.WEST);
        bottom.add(result, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        submit.setEnabled(false);
        showEmptyState();
        ctx.onQuizRequested(this::prepareFor);
    }

    @Override public String title() { return "Quiz"; }

    /** Pre-fills a short quiz on one concept (from the concept map) and generates it. */
    public void prepareFor(String conceptName) {
        concepts.setText(conceptName);
        count.setValue(3);
        result.setText("Quiz on " + conceptName + "...");
        requestQuiz();
    }

    @Override
    public void render() {
        if (quiz != null && ctx.course() != null && !quiz.getCourseId().equals(ctx.course().getId())) displayQuiz(null);
        if (quiz == null) result.setText(ctx.course() == null ? "Select a course first." : "Choose options and press Generate quiz.");
    }

    public void requestQuiz() {
        Course c = requireCourse();
        if (c == null) return;
        List<QuestionType> types = new ArrayList<>();
        if (mcq.isSelected()) types.add(QuestionType.MULTIPLE_CHOICE);
        if (tf.isSelected()) types.add(QuestionType.TRUE_FALSE);
        if (shortAnswer.isSelected()) types.add(QuestionType.SHORT_ANSWER);
        if (types.isEmpty()) {
            showError("Select at least one question type.");
            return;
        }
        List<String> names = Arrays.stream(concepts.getText().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        QuizRequest request;
        try {
            request = new QuizRequest(ctx.studentId, c.getId(), names, (Integer) count.getValue(), types,
                    (DifficultyLevel) difficulty.getSelectedItem());
        } catch (ValidationException e) {
            showError(e.getMessage());
            return;
        }
        runAsync("Generating and validating questions (AI)", () -> facade.generateQuiz(request), this::displayQuiz);
    }

    public void displayQuiz(Quiz q) {
        quiz = q;
        attempt = null;
        panels.clear();
        questions.removeAll();
        if (q != null) {
            for (int i = 0; i < q.getQuestions().size(); i++) {
                QuestionPanel p = new QuestionPanel(i + 1, q.getQuestions().get(i));
                p.onAnswerChanged(this::updateProgress);
                p.setAlignmentX(LEFT_ALIGNMENT);
                panels.add(p);
                questions.add(p);
                questions.add(Box.createVerticalStrut(14));
            }
            result.setText(q.getNotice() == null ? q.getQuestions().size() + " question(s). Answer them, then submit." : q.getNotice());
        } else {
            showEmptyState();
        }
        submit.setEnabled(q != null);
        answered.setVisible(q != null);
        updateProgress();
        questions.revalidate();
        questions.repaint();
        // Wrapped text knows its height only once it has a width: lay out once more after the first pass.
        SwingUtilities.invokeLater(() -> {
            questions.revalidate();
            questions.repaint();
        });
    }

    private void updateProgress() {
        int done = (int) panels.stream().filter(p -> !p.answer().isBlank()).count();
        answered.setMaximum(Math.max(1, panels.size()));
        answered.setValue(done);
        answered.setString(done + " of " + panels.size() + " answered");
    }

    private void showEmptyState() {
        questions.removeAll();
        Card empty = new Card(new BorderLayout(0, 6));
        empty.setBorder(BorderFactory.createEmptyBorder(36, 24, 36, 24));
        JLabel icon = new JLabel("\u2726", SwingConstants.CENTER);
        icon.setFont(icon.getFont().deriveFont(34f));
        icon.setForeground(Theme.ACCENT);
        JLabel title = new JLabel("Test yourself on your notes", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 19f));
        JLabel sub = new JLabel("<html><div style='text-align:center'>Pick how many questions and how hard, then press "
                + "<b>Generate quiz</b>.<br>Every question comes from your uploaded material, and wrong answers come with an "
                + "explanation.</div></html>", SwingConstants.CENTER);
        sub.setForeground(Theme.muted());
        empty.add(icon, BorderLayout.NORTH);
        empty.add(title, BorderLayout.CENTER);
        empty.add(sub, BorderLayout.SOUTH);
        empty.setAlignmentX(LEFT_ALIGNMENT);
        questions.add(empty);
        questions.revalidate();
        questions.repaint();
    }

    /** Big score card shown above the graded questions. */
    private Card scoreCard(QuizAttempt a) {
        double score = a.computeScore();
        Color tone = Theme.forMastery(score);
        Card card = new Card(new BorderLayout(18, 0), tone);
        card.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        JLabel pct = new JLabel(Math.round(score * 100) + "%");
        pct.setFont(pct.getFont().deriveFont(Font.BOLD, 40f));
        pct.setForeground(tone);
        JLabel headline = new JLabel(score >= 0.8 ? "Excellent work!" : score >= 0.5 ? "Good effort - almost there"
                : "Keep going - every mistake is a lesson");
        headline.setFont(headline.getFont().deriveFont(Font.BOLD, 17f));
        JLabel detail = new JLabel(a.correctCount() + " of " + a.getResults().size() + " correct.  "
                + (score >= 0.8 ? "Try a harder difficulty next." : "Click Explain on any question to see why."));
        detail.setForeground(Theme.muted());
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue((int) Math.round(score * 100));
        bar.setForeground(tone);
        bar.setPreferredSize(new Dimension(10, 8));
        JPanel text = new JPanel(new BorderLayout(0, 6));
        text.setOpaque(false);
        text.add(headline, BorderLayout.NORTH);
        text.add(detail, BorderLayout.CENTER);
        text.add(bar, BorderLayout.SOUTH);
        card.add(pct, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        card.setAlignmentX(LEFT_ALIGNMENT);
        return card;
    }

    /** Panel that follows the scroll pane's width so long questions wrap instead of running off-screen. */
    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 40; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    public void submitAnswers() {
        if (quiz == null) return;
        List<QuestionResponse> responses = panels.stream().map(QuestionPanel::response).toList();
        long blank = responses.stream().filter(r -> r.answerText.isBlank()).count();
        if (blank > 0 && !confirm(blank + " question(s) are unanswered and will be marked wrong. Submit anyway?")) return;
        submit.setEnabled(false);
        runAsync("Grading", () -> facade.submitQuiz(quiz.getId(), responses), this::displayResult, () -> submit.setEnabled(attempt == null));
    }

    public void displayResult(QuizAttempt a) {
        attempt = a;
        result.setText(String.format("Score %.0f%%  (%d of %d correct). Click Explain on any question for an AI explanation.",
                a.computeScore() * 100, a.correctCount(), a.getResults().size()));
        for (QuestionPanel p : panels) {
            a.findResult(p.question().getId()).ifPresent(p::showResult);
            p.addAction(button("Explain why", () -> requestExplanation(p.question().getId())));
        }
        questions.add(scoreCard(a), 0);
        questions.add(Box.createVerticalStrut(14), 1);
        answered.setVisible(false);
        questions.revalidate();
        questions.repaint();
        SwingUtilities.invokeLater(() -> questions.scrollRectToVisible(new Rectangle(0, 0, 1, 1)));
        ctx.fireDataChanged();
    }

    public void requestExplanation(String questionId) {
        if (attempt == null) return;
        runAsync("Explaining (AI)", () -> facade.explainAnswer(attempt.getId(), questionId), this::showExplanation);
    }

    private void showExplanation(Explanation e) {
        String sources = e.sourceRefs.isEmpty() ? "" : "\n\nSources:\n" + String.join("\n", e.sourceRefs);
        javax.swing.JTextArea area = readOnlyArea();
        area.setText(e.text + sources);
        area.setRows(12);
        area.setColumns(60);
        javax.swing.JOptionPane.showMessageDialog(this, scroll(area), "Explanation", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    List<QuestionPanel> panels() { return panels; }
    JLabel resultLabel() { return result; }
    JTextField conceptsField() { return concepts; }
}
