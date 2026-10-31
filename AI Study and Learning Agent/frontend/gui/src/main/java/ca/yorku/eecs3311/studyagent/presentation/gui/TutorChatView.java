package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Answer;
import ca.yorku.eecs3311.studyagent.domain.Course;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** F05: conversation with the tutor; answers cite the course material and follow-ups use session memory. */
public class TutorChatView extends BaseView {
    private final JTextArea transcript = readOnlyArea();
    private final JTextField input = new JTextField();
    private final JCheckBox showTrace = new JCheckBox("Show agent plan");
    private final JButton send = button("Ask", () -> sendQuestion(input.getText()));
    private String sessionId;

    public TutorChatView(GuiContext ctx) {
        super(ctx);
        sessionId = facade.newSessionId();
        input.addActionListener(e -> sendQuestion(input.getText()));
        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(row(send, button("New conversation", this::newConversation), showTrace), BorderLayout.EAST);
        add(heading("Ask the tutor about your course material"), BorderLayout.NORTH);
        add(scroll(transcript), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        ctx.onCourseChanged(c -> newConversation());
    }

    @Override public String title() { return "Tutor"; }

    @Override
    public void render() {
        if (transcript.getText().isEmpty()) {
            Course c = ctx.course();
            transcript.setText(c == null ? "Select a course first." : "Ask anything about " + c.getCode()
                    + ". Answers only use your uploaded material and cite it as [n].\n");
        }
        input.requestFocusInWindow();
    }

    public void sendQuestion(String text) {
        Course c = requireCourse();
        if (c == null || text == null || text.isBlank()) return;
        transcript.append("\nYou: " + text.trim() + "\n");
        input.setText("");
        send.setEnabled(false);
        runAsync("Tutor is thinking", () -> facade.askQuestion(sessionId, c.getId(), text.trim()), this::displayAnswer,
                () -> send.setEnabled(true));
    }

    public void displayAnswer(Answer a) {
        transcript.append("\nTutor: " + a.text + "\n");
        if (!a.citations.isEmpty()) transcript.append("Sources: " + String.join("; ", a.citations) + "\n");
        if (showTrace.isSelected()) transcript.append("\n" + facade.getLastAgentTrace() + "\n");
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }

    private void newConversation() {
        sessionId = facade.newSessionId();
        transcript.setText("");
        send.setEnabled(true);
        render();
    }

    JTextArea transcript() { return transcript; }
}
