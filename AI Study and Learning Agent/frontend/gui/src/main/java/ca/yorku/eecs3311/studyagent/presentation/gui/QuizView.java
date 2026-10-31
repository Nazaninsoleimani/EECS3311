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

/** F08 generate a quiz, F09 answer and grade it, F10 explain any graded answer. */
public class QuizView extends BaseView {
    private final JSpinner count = new JSpinner(new SpinnerNumberModel(5, 1, 30, 1));
    private final JComboBox<DifficultyLevel> difficulty = new JComboBox<>(DifficultyLevel.values());
    private final JCheckBox mcq = new JCheckBox("Multiple choice", true);
    private final JCheckBox tf = new JCheckBox("True/false", true);
    private final JCheckBox shortAnswer = new JCheckBox("Short answer", true);
    private final JTextField concepts = new JTextField(18);
    private final JPanel questions = new JPanel();
    private final JLabel result = new JLabel(" ");
    private final JButton submit = button("Submit answers", this::submitAnswers);
    private final List<QuestionPanel> panels = new ArrayList<>();
    private Quiz quiz;
    private QuizAttempt attempt;

    public QuizView(GuiContext ctx) {
        super(ctx);
        difficulty.setSelectedItem(DifficultyLevel.INTERMEDIATE);
        questions.setLayout(new BoxLayout(questions, BoxLayout.Y_AXIS));
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading("Practice quiz"), BorderLayout.NORTH);
        top.add(row(new JLabel("Questions"), count, new JLabel("Difficulty"), difficulty, mcq, tf, shortAnswer,
                new JLabel("Concepts (optional)"), concepts, button("Generate quiz", this::requestQuiz)), BorderLayout.SOUTH);
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(questions, BorderLayout.NORTH);
        add(top, BorderLayout.NORTH);
        add(scroll(holder), BorderLayout.CENTER);
        add(row(submit, result), BorderLayout.SOUTH);
        submit.setEnabled(false);
    }

    @Override public String title() { return "Quiz"; }

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
                panels.add(p);
                questions.add(p);
            }
            result.setText(q.getNotice() == null ? q.getQuestions().size() + " question(s). Answer them, then submit." : q.getNotice());
        }
        submit.setEnabled(q != null);
        questions.revalidate();
        questions.repaint();
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
            p.addAction(button("Explain", () -> requestExplanation(p.question().getId())));
        }
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
}
