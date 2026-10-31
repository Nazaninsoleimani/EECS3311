package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.application.ActivityOutcome;
import ca.yorku.eecs3311.studyagent.domain.AdaptiveLearningSession;
import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.Question;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

/**
 * F12: adaptive practice. Each answer is graded, the session's DifficultyState decides whether the
 * level goes up or down, and the agent picks the next concept and question type.
 */
public class LearningSessionView extends BaseView {
    private final JLabel level = new JLabel("Not started");
    private final JPanel questionHolder = new JPanel(new BorderLayout());
    private final JTextArea log = readOnlyArea();
    private final JButton start = button("Start session", this::startSession);
    private final JButton submit = button("Submit answer", this::submitActivityAnswer);
    private final JButton end = button("End session", this::endSession);
    private AdaptiveLearningSession session;
    private QuestionPanel current;

    public LearningSessionView(GuiContext ctx) {
        super(ctx);
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading("Adaptive learning session"), BorderLayout.NORTH);
        top.add(row(start, end, new JLabel("   Level:"), level), BorderLayout.SOUTH);
        JPanel center = new JPanel(new BorderLayout());
        center.add(questionHolder, BorderLayout.NORTH);
        center.add(row(submit), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll(center), scroll(log));
        split.setResizeWeight(0.55);
        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        updateButtons();
        ctx.onCourseChanged(c -> { if (session != null) endSession(); });
    }

    @Override public String title() { return "Learning Session"; }

    @Override
    public void render() {
        if (session == null && log.getText().isEmpty()) {
            log.setText("Questions start at your preferred level. Three correct in a row move you up a level;\n"
                    + "two wrong in a row move you down. The agent targets your weakest concepts first.\n");
        }
    }

    public void startSession() {
        Course c = requireCourse();
        if (c == null) return;
        start.setEnabled(false);
        runAsync("Starting session (agent picks your weakest concept)", () -> facade.startAdaptiveSession(ctx.studentId, c.getId()), s -> {
            session = s;
            log.append("\nSession started at " + s.getLevel() + ".\n");
            displayActivity(s.getCurrentQuestion());
        }, this::updateButtons);
    }

    public void displayActivity(Question q) {
        questionHolder.removeAll();
        current = q == null ? null : new QuestionPanel(session == null ? 1 : session.getAnswered() + 1, q);
        if (current != null) questionHolder.add(current, BorderLayout.CENTER);
        level.setText(session == null ? "Not started" : session.getLevel() + "  (" + session.getCorrect() + "/" + session.getAnswered() + " correct)");
        questionHolder.revalidate();
        questionHolder.repaint();
        updateButtons();
    }

    public void submitActivityAnswer() {
        if (session == null || current == null) return;
        if (current.answer().isBlank()) {
            showError("Choose or type an answer first.");
            return;
        }
        QuestionPanel answered = current;
        submit.setEnabled(false);
        runAsync("Grading and choosing the next question", () -> facade.submitActivityResult(session.getId(), answered.response()),
                this::showOutcome, this::updateButtons);
    }

    void showOutcome(ActivityOutcome out) {
        log.append((out.result().correct ? "Correct: " : "Incorrect: ") + out.result().feedback + "\n");
        if (out.levelChanged()) log.append(">> Level " + out.previousLevel() + " -> " + out.level() + "\n");
        if (out.notice() != null) log.append(out.notice() + "\n");
        log.setCaretPosition(log.getDocument().getLength());
        displayActivity(out.next());
        ctx.fireDataChanged();
    }

    private void endSession() {
        if (session == null) return;
        attempt(() -> {
            AdaptiveLearningSession s = facade.endAdaptiveSession(session.getId());
            log.append(String.format("%nSession ended: %d of %d correct. Levels: %s%n", s.getCorrect(), s.getAnswered(),
                    String.join(" -> ", s.getLevelHistory())));
        });
        session = null;
        displayActivity(null);
    }

    private void updateButtons() {
        start.setEnabled(session == null);
        end.setEnabled(session != null);
        submit.setEnabled(session != null && current != null);
    }

    AdaptiveLearningSession session() { return session; }
    QuestionPanel currentQuestion() { return current; }
    JTextArea logArea() { return log; }
    void sessionForTest(AdaptiveLearningSession s) { session = s; displayActivity(s.getCurrentQuestion()); }
}
