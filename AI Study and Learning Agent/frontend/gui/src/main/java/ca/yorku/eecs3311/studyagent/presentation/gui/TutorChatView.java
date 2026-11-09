package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Answer;
import ca.yorku.eecs3311.studyagent.domain.Course;
import java.awt.BorderLayout;
import java.awt.Color;
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

    /**
     * The conversation as HTML: student bubbles on the right in the accent colour; tutor answers as cards with a
     * header (grounded or not), formatted Markdown with citation tags, source chips and, optionally, the agent plan.
     */
    String html() {
        String text = Theme.hex(Theme.text());
        String muted = Theme.hex(Theme.muted());
        String accent = Theme.hex(Theme.ACCENT);
        StringBuilder sb = new StringBuilder("<html><body style='font-family:sans-serif; font-size:13pt; color:" + text
                + "; margin:14px 16px 14px 16px'>");
        Course c = ctx.course();
        if (messages.isEmpty()) appendWelcome(sb, c, text, muted);
        for (Message m : messages) {
            if (m.fromStudent()) appendStudent(sb, m, accent, muted);
            else appendTutor(sb, m, muted);
        }
        if (thinking) {
            sb.append(tutorHeader(null, muted))
              .append("<table cellpadding='12' cellspacing='0'><tr><td bgcolor='").append(Theme.hex(Theme.tint(Theme.ACCENT, 0.07)))
              .append("'><font color='").append(muted).append("'><i>Searching your notes and writing an answer")
              .append(".".repeat(dots)).append("</i></font></td></tr></table>");
        }
        return sb.append("</body></html>").toString();
    }

    private void appendWelcome(StringBuilder sb, Course c, String text, String muted) {
        sb.append("<div style='text-align:center; margin-top:30px'>");
        if (c == null) {
            sb.append("<font color='").append(muted).append("'>Select or create a course to start.</font></div>");
            return;
        }
        sb.append("<div style='font-size:28pt'><font color='").append(StudyNotesHtml.accentText()).append("'>&#10022;</font></div>")
          .append("<div style='font-size:18pt; margin-top:4px'><b>Ask anything about ").append(esc(c.getCode())).append("</b></div>")
          .append("<div style='color:").append(muted).append("; margin-top:6px'>Every answer comes from your own notes, ")
          .append("with the pages it used.</div><br>")
          .append("<div style='color:").append(muted).append("; font-size:11pt'><b>TRY ONE OF THESE</b></div></div>")
          .append("<table align='center' cellpadding='0' cellspacing='8'>");
        for (String q : SUGGESTIONS) {
            sb.append("<tr><td bgcolor='").append(Theme.hex(Theme.tint(Theme.ACCENT, 0.08))).append("'>")
              .append("<table cellpadding='12' cellspacing='0'><tr><td><a href='ask:").append(esc(q)).append("'><font color='")
              .append(StudyNotesHtml.accentText()).append("'>").append(esc(q)).append("&nbsp; &rarr;</font></a></td></tr></table></td></tr>");
        }
        sb.append("</table>");
    }

    private static void appendStudent(StringBuilder sb, Message m, String accent, String muted) {
        sb.append("<table width='100%' cellspacing='0' cellpadding='0' style='margin-bottom:14px'><tr><td width='20%'></td>")
          .append("<td align='right'><div style='font-size:10.5pt; color:").append(muted).append("; margin-bottom:3px'><b>You</b></div>")
          .append("<table cellpadding='12' cellspacing='0'><tr><td bgcolor='").append(accent)
          .append("'><font color='#ffffff'>").append(esc(m.text())).append("</font></td></tr></table></td></tr></table>");
    }

    private static void appendTutor(StringBuilder sb, Message m, String muted) {
        Color tone = m.warning() ? Theme.WARNING : Theme.ACCENT;
        sb.append(tutorHeader(m, muted))
          .append("<table width='94%' cellpadding='0' cellspacing='0' style='margin-bottom:16px'><tr>")
          .append("<td width='4' bgcolor='").append(Theme.hex(tone)).append("'></td>")
          .append("<td bgcolor='").append(Theme.hex(Theme.tint(tone, m.warning() ? 0.10 : 0.06))).append("'>")
          .append("<table width='100%' cellpadding='14' cellspacing='0'><tr><td style='font-size:13.5pt'>")
          .append(StudyNotesHtml.markdown(m.text()));
        if (!m.sources().isEmpty()) {
            sb.append("<div style='margin-top:6px; margin-bottom:4px; font-size:10.5pt'><font color='").append(muted)
              .append("'><b>Sources</b></font></div><table cellpadding='0' cellspacing='4'><tr>");
            int col = 0;
            for (String s : m.sources()) {
                if (col > 0 && col % 2 == 0) sb.append("</tr><tr>");
                sb.append("<td bgcolor='").append(Theme.hex(Theme.tint(Theme.ACCENT, 0.14))).append("'>")
                  .append("<table cellpadding='5' cellspacing='0'><tr><td style='font-size:11pt'><font color='")
                  .append(StudyNotesHtml.accentText()).append("'>&#9632;</font>&nbsp; ").append(esc(s)).append("</td></tr></table></td>");
                col++;
            }
            sb.append("</tr></table>");
        }
        if (m.trace() != null) {
            sb.append("<table width='100%' cellpadding='10' cellspacing='0' style='margin-top:10px'><tr><td bgcolor='")
              .append(Theme.hex(Theme.tint(Theme.text(), 0.05))).append("'>")
              .append("<div style='font-size:10.5pt; margin-bottom:4px'><font color='").append(muted).append("'><b>&#9881; AGENT PLAN</b></font></div>")
              .append("<span style='font-family:monospace; font-size:10pt; color:").append(muted).append("'>")
              .append(esc(m.trace())).append("</span></td></tr></table>");
        }
        sb.append("</td></tr></table></td></tr></table>");
    }

    /** "AI  Study Tutor  [grounded tag]" above an answer; m == null while the tutor is still thinking. */
    private static String tutorHeader(Message m, String muted) {
        StringBuilder sb = new StringBuilder("<table cellpadding='0' cellspacing='0' style='margin-bottom:5px'><tr>")
                .append("<td bgcolor='").append(Theme.hex(Theme.ACCENT)).append("'><table cellpadding='4' cellspacing='0'><tr><td>")
                .append("<font color='#ffffff'><b style='font-size:10pt'>&nbsp;AI&nbsp;</b></font></td></tr></table></td>")
                .append("<td>&nbsp;&nbsp;<b>Study Tutor</b>&nbsp;&nbsp;</td>");
        if (m != null) {
            Color tone = m.warning() ? Theme.WARNING : Theme.SUCCESS;
            String label = m.warning() ? "! Not found in your material" : "&#10003; Grounded in your notes";
            sb.append("<td bgcolor='").append(Theme.hex(Theme.tint(tone, 0.16))).append("'><table cellpadding='3' cellspacing='0'><tr><td>")
              .append("<font color='").append(Theme.hex(tone)).append("'><b style='font-size:10pt'>&nbsp;").append(label)
              .append("&nbsp;</b></font></td></tr></table></td>");
        }
        return sb.append("</tr></table>").toString();
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
