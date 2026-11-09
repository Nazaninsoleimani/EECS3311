package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Answer;
import ca.yorku.eecs3311.studyagent.domain.Course;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;

/**
 * F05: chat with the tutor. Answers only use the uploaded material and cite it; follow-up questions
 * use the session memory. Messages are shown as bubbles; suggested questions can be clicked.
 */
public class TutorChatView extends BaseView {
    static final List<String> SUGGESTIONS = List.of("What are the key concepts of this course?",
            "Explain the most important idea in simple terms.", "Give me an example and explain it step by step.");

    private record Message(boolean fromStudent, String text, List<String> sources, String trace, boolean warning) {}

    private final List<Message> messages = new ArrayList<>();
    private final JEditorPane transcript = new JEditorPane("text/html", "");
    private final JTextField input = new JTextField();
    private final JCheckBox showTrace = new JCheckBox("Show agent plan");
    private final JButton send = accentButton("Ask", () -> sendQuestion(input.getText()));
    private boolean thinking;
    private int dots;
    private final javax.swing.Timer thinkingTimer = new javax.swing.Timer(400, e -> {
        dots = (dots + 1) % 4;
        refresh();
    });
    private String sessionId;

    public TutorChatView(GuiContext ctx) {
        super(ctx);
        sessionId = facade.newSessionId();
        transcript.setEditable(false);
        transcript.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        transcript.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getDescription().startsWith("ask:")) {
                sendQuestion(e.getDescription().substring(4));
            }
        });
        input.putClientProperty("JTextField.placeholderText", "Ask a question about your notes, e.g. \"Why is Observer useful?\"");
        input.putClientProperty("FlatLaf.style", "margin: 6,10,6,10");
        input.addActionListener(e -> sendQuestion(input.getText()));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(heading("Tutor"), BorderLayout.NORTH);
        top.add(hint("Answers come only from your uploaded material and cite it as [1], [2] ... Follow-up questions remember the conversation."),
                BorderLayout.SOUTH);
        JScrollPane sp = new JScrollPane(transcript);
        sp.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Theme.border()));
        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(row(send, button("New conversation", this::newConversation), showTrace), BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        ctx.onCourseChanged(c -> newConversation());
    }

    @Override public String title() { return "Tutor"; }

    @Override
    public void render() {
        refresh();
        input.requestFocusInWindow();
    }

    public void sendQuestion(String text) {
        Course c = requireCourse();
        if (c == null || text == null || text.isBlank() || thinking) return;
        messages.add(new Message(true, text.trim(), List.of(), null, false));
        input.setText("");
        setThinking(true);
        runAsync("Tutor is thinking", () -> facade.askQuestion(sessionId, c.getId(), text.trim()), this::displayAnswer,
                () -> setThinking(false));
    }

    public void displayAnswer(Answer a) {
        messages.add(new Message(false, a.text, a.citations, showTrace.isSelected() ? facade.getLastAgentTrace() : null, !a.grounded));
        refresh();
    }

    private void setThinking(boolean on) {
        thinking = on;
        dots = 0;
        if (on) thinkingTimer.start(); else thinkingTimer.stop();
        send.setEnabled(!on);
        refresh();
    }

    private void newConversation() {
        sessionId = facade.newSessionId();
        messages.clear();
        setThinking(false);
    }

    private void refresh() {
        transcript.setText(html());
        SwingUtilities.invokeLater(() -> transcript.setCaretPosition(transcript.getDocument().getLength()));
    }

    /** The conversation as HTML: student bubbles on the right in the accent colour, tutor bubbles on the left. */
    String html() {
        String text = Theme.hex(Theme.text());
        String muted = Theme.hex(Theme.muted());
        String tutorBg = Theme.hex(Theme.tint(Theme.ACCENT, 0.07));
        StringBuilder sb = new StringBuilder("<html><body style='font-family:sans-serif; font-size:13pt; color:" + text
                + "; margin:12px'>");
        Course c = ctx.course();
        if (messages.isEmpty()) {
            sb.append("<div style='text-align:center; color:").append(muted).append("; margin-top:40px'>");
            if (c == null) {
                sb.append("Select or create a course to start.");
            } else {
                sb.append("<span style='font-size:16pt; color:").append(text).append("'><b>Ask anything about ")
                  .append(esc(c.getCode())).append("</b></span><br><br>Try one of these:<br><br>");
                for (String q : SUGGESTIONS) {
                    sb.append("<a href='ask:").append(esc(q)).append("'>").append(esc(q)).append("</a><br><br>");
                }
            }
            sb.append("</div>");
        }
        for (Message m : messages) {
            if (m.fromStudent()) {
                sb.append("<table width='100%' cellspacing='0' cellpadding='0'><tr><td width='25%'></td>")
                  .append("<td align='right'><table cellpadding='10' cellspacing='0'><tr><td bgcolor='").append(Theme.hex(Theme.ACCENT))
                  .append("' style='color:#ffffff'>").append(esc(m.text())).append("</td></tr></table></td></tr></table><br>");
            } else {
                sb.append("<table width='100%' cellspacing='0' cellpadding='0'><tr><td>")
                  .append("<span style='color:").append(muted).append("; font-size:11pt'><b>Tutor</b>")
                  .append(m.warning() ? " &nbsp;· not verified against your material" : "").append("</span>")
                  .append("<table cellpadding='10' cellspacing='0' width='92%'><tr><td bgcolor='").append(tutorBg).append("'>")
                  .append(esc(m.text()));
                if (!m.sources().isEmpty()) {
                    sb.append("<br><br><span style='font-size:11pt; color:").append(muted).append("'><b>Sources</b><br>");
                    for (String s : m.sources()) sb.append("&nbsp;").append(esc(s)).append("<br>");
                    sb.append("</span>");
                }
                if (m.trace() != null) {
                    sb.append("<br><span style='font-family:monospace; font-size:10pt; color:").append(muted).append("'>")
                      .append(esc(m.trace())).append("</span>");
                }
                sb.append("</td></tr></table></td></tr></table><br>");
            }
        }
        if (thinking) sb.append("<span style='color:").append(muted).append("'><i>Tutor is thinking" + ".".repeat(dots) + "</i></span>");
        return sb.append("</body></html>").toString();
    }

    private static String esc(String s) {
        return QuestionPanel.escape(s).replace("'", "&#39;").replace("\n", "<br>");
    }

    /** Shows a question and its answer without the background call (tests and screenshots). */
    void showExchange(String question, Answer a) {
        messages.add(new Message(true, question, List.of(), null, false));
        displayAnswer(a);
    }

    /** The conversation as plain text (what a reader sees), for tests. */
    String transcriptText() {
        try {
            return transcript.getDocument().getText(0, transcript.getDocument().getLength());
        } catch (BadLocationException e) {
            return "";
        }
    }
}
