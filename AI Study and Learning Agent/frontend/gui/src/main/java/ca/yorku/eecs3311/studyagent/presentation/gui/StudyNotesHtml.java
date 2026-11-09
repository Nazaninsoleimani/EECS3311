package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Concept;
import ca.yorku.eecs3311.studyagent.domain.ConceptGraph;
import ca.yorku.eecs3311.studyagent.domain.Summary;
import ca.yorku.eecs3311.studyagent.domain.WeakTopic;
import ca.yorku.eecs3311.studyagent.domain.WeakTopicReport;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the HTML shown in the Materials tab's reading pane: study-ready summaries, extracted concepts and status
 * cards. Colours come from {@link Theme} at build time, so rebuilding after a theme switch restyles the page.
 * Only the HTML 3.2 / CSS subset that Swing renders is used (tables with bgcolor for cards and accent bars).
 */
final class StudyNotesHtml {
    enum Kind { SUCCESS, INFO, WARNING, ERROR }

    /** Link targets handled by MaterialView. */
    static final String NAV_QUIZ = "nav:quiz", NAV_FLASHCARDS = "nav:flashcards", NAV_TUTOR = "nav:tutor",
            NAV_MAP = "nav:map", NAV_SESSION = "nav:session", NAV_MATERIALS = "nav:materials", NAV_HISTORY = "nav:history",
            QUIZ_ON = "quizon:";

    /** Follows a link from one of these pages: switches tab, or opens a quiz on a concept. */
    static void follow(GuiContext ctx, String target) {
        if (target == null) return;
        if (target.startsWith(QUIZ_ON)) {
            ctx.requestQuizOn(target.substring(QUIZ_ON.length()));
            return;
        }
        switch (target) {
            case NAV_QUIZ -> ctx.navigateTo(QuizView.class);
            case NAV_FLASHCARDS -> ctx.navigateTo(FlashcardView.class);
            case NAV_TUTOR -> ctx.navigateTo(TutorChatView.class);
            case NAV_MAP -> ctx.navigateTo(ConceptMapView.class);
            case NAV_SESSION -> ctx.navigateTo(LearningSessionView.class);
            case NAV_MATERIALS -> ctx.navigateTo(MaterialView.class);
            case NAV_HISTORY -> ctx.navigateTo(HistoryView.class);
            default -> { }
        }
    }

    private StudyNotesHtml() {}

    // ------------------------------------------------------------------ pages

    static String summary(Summary s, String materialTitle) {
        String accent = hex(Theme.ACCENT);
        int words = s.getText().trim().isEmpty() ? 0 : s.getText().trim().split("\\s+").length;
        int minutes = Math.max(1, (int) Math.ceil(words / 200.0));
        StringBuilder sb = open();

        // Hero card: what this is and how long it takes.
        sb.append("<table width='100%' cellpadding='18' cellspacing='0'><tr><td bgcolor='").append(tint(0.12)).append("'>")
          .append(pill(s.getLength() + " SUMMARY", accent, "#ffffff"))
          .append("<div style='font-size:20pt; font-weight:bold; margin-top:8px'>").append(esc(displayTitle(materialTitle))).append("</div>")
          .append("<div style='color:").append(muted()).append("; margin-top:6px'>")
          .append(minutes).append(" min read &nbsp;&middot;&nbsp; ").append(s.getKeyPoints().size()).append(" key point")
          .append(s.getKeyPoints().size() == 1 ? "" : "s")
          .append(s.getSourceRefs().isEmpty() ? "" : " &nbsp;&middot;&nbsp; " + s.getSourceRefs().size() + " source"
                  + (s.getSourceRefs().size() == 1 ? "" : "s"))
          .append("</div></td></tr></table>");

        List<List<String>> topics = topics(s.getText());
        if (topics.size() == 1 && topics.get(0).size() <= 2) {
            // A short summary reads best as one paragraph.
            sb.append(sectionTitle("The big picture"));
            sb.append("<div style='font-size:14pt; margin-right:8px'>").append(richText(s.getText())).append("</div>");
        } else {
            sb.append(sectionTitle(topics.size() == 1 ? "The big picture" : "The big picture in " + topics.size() + " topics"));
            for (int i = 0; i < topics.size(); i++) sb.append(topicCard(i + 1, topics.get(i)));
        }

        if (!s.getKeyPoints().isEmpty()) {
            sb.append(sectionTitle("Key points to remember"));
            List<String> points = s.getKeyPoints();
            for (int i = 0; i < points.size(); i++) {
                sb.append("<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:6px'><tr>")
                  .append("<td width='5' bgcolor='").append(accent).append("'></td>")
                  .append("<td bgcolor='").append(tint(0.06)).append("'>")
                  .append("<table cellpadding='10' cellspacing='0'><tr>")
                  .append("<td valign='top' width='34'><font color='").append(accentText()).append("'><b style='font-size:15pt'>")
                  .append(String.format("%02d", i + 1)).append("</b></font></td>")
                  .append("<td valign='top' style='font-size:13pt'>").append(richText(points.get(i))).append("</td>")
                  .append("</tr></table></td></tr></table>");
            }
        }

        if (!s.getSourceRefs().isEmpty()) {
            sb.append(sectionTitle("Where this comes from"));
            sb.append("<table cellpadding='0' cellspacing='6'><tr>");
            int col = 0;
            for (String ref : s.getSourceRefs()) {
                if (col > 0 && col % 3 == 0) sb.append("</tr><tr>");
                sb.append("<td bgcolor='").append(tint(0.10)).append("'><table cellpadding='6' cellspacing='0'><tr><td>")
                  .append("<font color='").append(accentText()).append("'>&#9632;</font>&nbsp; <span style='font-size:11pt'>")
                  .append(esc(ref)).append("</span></td></tr></table></td>");
                col++;
            }
            sb.append("</tr></table>");
        }

        sb.append(nextSteps());
        return close(sb);
    }

    static String concepts(ConceptGraph g, String materialTitle, String lead) {
        String accent = hex(Theme.ACCENT);
        StringBuilder sb = open();
        if (lead != null) sb.append(card(Kind.SUCCESS, lead, null));
        sb.append("<table width='100%' cellpadding='18' cellspacing='0'><tr><td bgcolor='").append(tint(0.12)).append("'>")
          .append(pill("KEY CONCEPTS", accent, "#ffffff"))
          .append("<div style='font-size:20pt; font-weight:bold; margin-top:8px'>").append(esc(displayTitle(materialTitle))).append("</div>")
          .append("<div style='color:").append(muted()).append("; margin-top:6px'>").append(g.nodes.size()).append(" concept")
          .append(g.nodes.size() == 1 ? "" : "s").append(" &nbsp;&middot;&nbsp; ").append(g.edges.size()).append(" relation")
          .append(g.edges.size() == 1 ? "" : "s").append(" &nbsp;&middot;&nbsp; click <i>Quiz me</i> on any concept to practise it")
          .append("</div></td></tr></table><br>");

        sb.append("<table width='100%' cellpadding='0' cellspacing='8'>");
        for (int i = 0; i < g.nodes.size(); i++) {
            if (i % 2 == 0) sb.append("<tr>");
            Concept c = g.nodes.get(i);
            sb.append("<td width='50%' valign='top' bgcolor='").append(tint(0.06)).append("'>")
              .append("<table width='100%' cellpadding='12' cellspacing='0'><tr><td>")
              .append("<b style='font-size:14pt'>").append(esc(c.getName())).append("</b>");
            if (!c.getDefinition().isBlank()) {
                sb.append("<div style='color:").append(muted()).append("; margin-top:4px'>").append(esc(c.getDefinition())).append("</div>");
            }
            sb.append("<div style='margin-top:8px'>").append(link(QUIZ_ON + attr(c.getName()), "Quiz me &rarr;")).append("</div>")
              .append("</td></tr></table></td>");
            if (i % 2 == 1) sb.append("</tr>");
        }
        if (g.nodes.size() % 2 == 1) sb.append("<td></td></tr>");
        sb.append("</table>");
        sb.append("<div style='margin-top:6px'>").append(link(NAV_MAP, "See how these concepts connect on the Concept Map &rarr;")).append("</div>");
        return close(sb);
    }

    // ------------------------------------------------------------------ weak topics (F11)

    static String weakTopics(WeakTopicReport r) {
        StringBuilder sb = open();
        int n = r.topics.size();
        String headline = n == 0 ? (r.sufficientData ? "No weak topics right now" : "Not enough practice yet")
                : n + " topic" + (n == 1 ? " needs" : "s need") + " your attention";
        sb.append("<table width='100%' cellpadding='18' cellspacing='0'><tr><td bgcolor='").append(tint(0.12)).append("'>")
          .append(pill("WEAK TOPIC ANALYSIS", hex(Theme.ACCENT), "#ffffff"))
          .append("<div style='font-size:20pt; font-weight:bold; margin-top:8px'>").append(esc(headline)).append("</div>")
          .append("<div style='color:").append(muted()).append("; margin-top:6px'>");
        if (n > 0) sb.append("Most urgent first").append(r.aiDiagnosed ? " &nbsp;&middot;&nbsp; diagnosed by the AI from your wrong answers and your notes" : "");
        else if (r.message != null) sb.append(esc(r.message));
        sb.append("</div></td></tr></table><br>");

        if (n == 0) {
            sb.append(card(r.sufficientData ? Kind.SUCCESS : Kind.INFO,
                    r.sufficientData ? "Nothing is below the weak threshold - great work!" : "Answer a few more questions first",
                    r.sufficientData ? "Keep practising to move every concept to mastered." :
                            "Weak topics are found after a concept has been practised a few times. Take a quiz or start a learning session."));
            return close(sb);
        }
        for (int i = 0; i < n; i++) sb.append(weakTopicCard(i + 1, r.topics.get(i)));
        if (r.message != null && !r.message.isBlank() && !r.aiDiagnosed) {
            sb.append("<div style='color:").append(muted()).append("; font-size:11pt'>").append(esc(r.message)).append("</div>");
        }
        return close(sb);
    }

    private static String weakTopicCard(int rank, WeakTopic t) {
        Color tone = Theme.forMastery(t.mastery);
        int pct = (int) Math.round(Math.max(0, Math.min(1, t.mastery)) * 100);
        StringBuilder sb = new StringBuilder("<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:12px'><tr>")
                .append("<td width='6' bgcolor='").append(hex(tone)).append("'></td>")
                .append("<td bgcolor='").append(hex(Theme.tint(tone, 0.07))).append("'><table width='100%' cellpadding='16' cellspacing='0'><tr><td>");
        // Rank, name and mastery.
        sb.append("<div style='font-size:10.5pt'><font color='").append(hex(tone)).append("'><b>#").append(rank)
          .append(rank == 1 ? " &nbsp;MOST URGENT" : "").append("</b></font></div>")
          .append("<table width='100%' cellpadding='0' cellspacing='0' style='margin-top:4px'><tr>")
          .append("<td valign='middle'><b style='font-size:17pt'>").append(esc(t.conceptName)).append("</b></td>")
          .append("<td align='right' valign='middle' width='260'><table cellpadding='0' cellspacing='0'><tr>")
          .append("<td style='font-size:11pt'><font color='").append(muted()).append("'>Mastery&nbsp;&nbsp;</font></td>")
          .append("<td width='150'>").append(bar(pct, tone, 150)).append("</td>")
          .append("<td style='font-size:13pt'>&nbsp;&nbsp;<font color='").append(hex(tone)).append("'><b>").append(pct).append("%</b></font></td>")
          .append("</tr></table></td></tr></table>");
        // Diagnosis.
        if (t.diagnosis != null && !t.diagnosis.isBlank()) {
            sb.append("<div style='margin-top:12px; font-size:10.5pt'><font color='").append(accentText()).append("'><b>WHAT IS GOING WRONG</b></font></div>")
              .append("<div style='margin-top:4px; font-size:13.5pt'>").append(richText(t.diagnosis)).append("</div>");
        }
        // Recommended next step as a call to action.
        String[] action = recommended(t);
        if (action != null) {
            sb.append("<table cellpadding='0' cellspacing='0' style='margin-top:12px'><tr><td bgcolor='").append(hex(Theme.ACCENT)).append("'>")
              .append("<table cellpadding='8' cellspacing='0'><tr><td><a href='").append(action[0]).append("'><font color='#ffffff'><b>")
              .append(action[1]).append(" &rarr;</b></font></a></td></tr></table></td>")
              .append("<td style='font-size:11pt'>&nbsp;&nbsp;<font color='").append(muted()).append("'>").append(action[2]).append("</font></td>")
              .append("</tr></table>");
        }
        return sb.append("</td></tr></table></td></tr></table>").toString();
    }

    /** {link, button text, why} for the recommended activity, or null. */
    private static String[] recommended(WeakTopic t) {
        if (t.recommendedActivity == null) return null;
        String name = attr(t.conceptName);
        return switch (t.recommendedActivity) {
            case ADAPTIVE_SESSION -> new String[]{NAV_SESSION, "Start a learning session", "adapts the difficulty as you answer"};
            case READ -> new String[]{NAV_MATERIALS, "Re-read the material", "summarize it, then test yourself"};
            case QUIZ -> new String[]{QUIZ_ON + name, "Quiz me on " + esc(t.conceptName), "a short quiz on just this concept"};
            case FLASHCARDS -> new String[]{NAV_FLASHCARDS, "Review flashcards", "spaced repetition locks it in"};
            case REVIEW -> new String[]{NAV_HISTORY, "Review past mistakes", "see what you answered before"};
        };
    }

    /** A horizontal bar of the given width (px) filled to pct percent. */
    private static String bar(int pct, Color tone, int width) {
        int filled = (int) Math.round(width * pct / 100.0);
        StringBuilder sb = new StringBuilder("<table cellpadding='0' cellspacing='0' width='").append(width).append("'><tr>");
        if (filled > 0) sb.append("<td width='").append(filled).append("' height='9' bgcolor='").append(hex(tone)).append("'></td>");
        if (filled < width) sb.append("<td width='").append(width - filled).append("' height='9' bgcolor='").append(hex(Theme.tint(Theme.text(), 0.12))).append("'></td>");
        return sb.append("</tr></table>").toString();
    }

    static String weakTopicsIntro(boolean hasCourse) {
        StringBuilder sb = open();
        sb.append("<div style='text-align:center; margin-top:18px'>")
          .append("<div style='font-size:28pt'><font color='").append(accentText()).append("'>&#9673;</font></div>")
          .append("<div style='font-size:17pt; font-weight:bold; margin-top:4px'>")
          .append(hasCourse ? "Find out what to study next" : "Select a course first").append("</div>");
        if (hasCourse) {
            sb.append("<div style='color:").append(muted()).append("; margin-top:8px'>Press <b>Analyze weak topics</b>: the agent looks at ")
              .append("your wrong answers, explains what is going wrong<br>and recommends the best next activity for each topic.</div>");
        }
        return close(sb.append("</div>"));
    }

    /** A page with one or more status cards. */
    static String message(Kind kind, String title, String body) {
        return close(open().append(card(kind, title, body)));
    }

    static String messages(String[]... cards) {
        StringBuilder sb = open();
        for (String[] c : cards) sb.append(card(Kind.valueOf(c[0]), c[1], c.length > 2 ? c[2] : null));
        return close(sb);
    }

    static String welcome(boolean hasCourse) {
        String accent = hex(Theme.ACCENT);
        StringBuilder sb = open();
        sb.append("<div style='text-align:center; margin-top:28px'>")
          .append("<div style='font-size:30pt'><font color='").append(accentText()).append("'>&#9998;</font></div>")
          .append("<div style='font-size:17pt; font-weight:bold; margin-top:6px'>")
          .append(hasCourse ? "Turn your notes into a study guide" : "Select a course to see its materials").append("</div>");
        if (hasCourse) {
            sb.append("<div style='color:").append(muted()).append("; margin-top:8px; font-size:12pt'>")
              .append("1. Upload your lecture notes &nbsp;&nbsp; 2. Select a material &nbsp;&nbsp; ")
              .append("3. Click <b>Summarize</b> or <b>Extract concepts</b></div>");
        }
        sb.append("</div>");
        return close(sb);
    }

    // ------------------------------------------------------------------ building blocks

    private static StringBuilder open() {
        return new StringBuilder("<html><body style='font-family:sans-serif; font-size:13pt; color:")
                .append(hex(Theme.text())).append("; background-color:").append(hex(Theme.card()))
                .append("; margin:16px 18px 16px 18px'>");
    }

    private static String close(StringBuilder sb) {
        return sb.append("</body></html>").toString();
    }

    private static String sectionTitle(String title) {
        return "<div style='margin-top:20px; margin-bottom:8px; font-size:11pt; font-weight:bold'><font color='"
                + accentText() + "'>" + esc(title.toUpperCase()) + "</font></div>";
    }

    private static String pill(String text, String bg, String fg) {
        return "<table cellpadding='4' cellspacing='0'><tr><td bgcolor='" + bg + "'><font color='" + fg
                + "'><b style='font-size:10pt'>&nbsp;" + esc(text) + "&nbsp;</b></font></td></tr></table>";
    }

    private static String card(Kind kind, String title, String body) {
        Color c = switch (kind) {
            case SUCCESS -> Theme.SUCCESS;
            case INFO -> Theme.ACCENT;
            case WARNING -> Theme.WARNING;
            case ERROR -> Theme.DANGER;
        };
        String icon = switch (kind) {
            case SUCCESS -> "&#10003;";
            case INFO -> "&#9679;";
            case WARNING, ERROR -> "!";
        };
        StringBuilder sb = new StringBuilder("<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:10px'><tr>")
                .append("<td width='5' bgcolor='").append(hex(c)).append("'></td>")
                .append("<td bgcolor='").append(hex(Theme.tint(c, 0.10))).append("'>")
                .append("<table cellpadding='12' cellspacing='0'><tr><td valign='top' width='22'><font color='").append(hex(c))
                .append("'><b style='font-size:15pt'>").append(icon).append("</b></font></td><td>")
                .append("<b style='font-size:14pt'>").append(esc(title)).append("</b>");
        if (body != null && !body.isBlank()) {
            sb.append("<div style='margin-top:4px; color:").append(muted()).append("'>").append(esc(body)).append("</div>");
        }
        return sb.append("</td></tr></table></td></tr></table>").toString();
    }

    private static String nextSteps() {
        return "<br><table width='100%' cellpadding='16' cellspacing='0'><tr><td bgcolor='" + tint(0.12) + "'>"
                + "<b style='font-size:14pt'>Ready to lock this in?</b>"
                + "<div style='color:" + muted() + "; margin-top:4px'>Students remember far more when they test themselves "
                + "right after reading.</div>"
                + "<div style='margin-top:10px; font-size:13pt'>"
                + link(NAV_QUIZ, "<b>Take a quiz</b>") + " &nbsp;&nbsp;&middot;&nbsp;&nbsp; "
                + link(NAV_FLASHCARDS, "<b>Review flashcards</b>") + " &nbsp;&nbsp;&middot;&nbsp;&nbsp; "
                + link(NAV_TUTOR, "<b>Ask the tutor</b>") + " &nbsp;&nbsp;&middot;&nbsp;&nbsp; "
                + link(NAV_MAP, "<b>Concept map</b>")
                + "</div></td></tr></table>";
    }

    // ------------------------------------------------------------------ making long text scannable

    /** Paragraphs (split on blank lines) as lists of sentences. No text is dropped. */
    static List<List<String>> topics(String text) {
        List<List<String>> out = new ArrayList<>();
        for (String para : (text == null ? "" : text.strip()).split("\\n\\s*\\n")) {
            String p = para.replaceAll("\\s*\\n\\s*", " ").strip();
            if (p.isEmpty()) continue;
            List<String> sentences = new ArrayList<>();
            for (String sentence : SENTENCE_END.split(p)) {
                if (!sentence.isBlank()) sentences.add(sentence.strip());
            }
            out.add(sentences);
        }
        return out;
    }

    /** Sentence boundary: . ! or ? followed by space and a capital letter, quote or bracket. */
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+(?=[A-Z\"'(\\[])");

    /** One paragraph as a card: its first sentence is the headline, the rest become bullet points. */
    private static String topicCard(int number, List<String> sentences) {
        StringBuilder sb = new StringBuilder("<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:10px'><tr>")
                .append("<td width='5' bgcolor='").append(hex(Theme.ACCENT)).append("'></td>")
                .append("<td bgcolor='").append(tint(0.05)).append("'><table width='100%' cellpadding='14' cellspacing='0'><tr><td>")
                .append("<table cellpadding='0' cellspacing='0'><tr>")
                .append("<td valign='top' width='40'><font color='").append(accentText()).append("'><b style='font-size:16pt'>")
                .append(String.format("%02d", number)).append("</b></font></td>")
                .append("<td valign='top' style='font-size:14.5pt'><b>").append(richText(sentences.get(0))).append("</b></td>")
                .append("</tr></table>");
        if (sentences.size() > 1) {
            sb.append("<table cellpadding='0' cellspacing='0' style='margin-top:8px'>");
            for (String s : sentences.subList(1, sentences.size())) {
                sb.append("<tr><td width='40'></td><td valign='top' width='18'><font color='").append(accentText())
                  .append("'><b>&#8250;</b></font></td><td valign='top' style='font-size:13.5pt; padding-bottom:5px'>")
                  .append(richText(s)).append("</td></tr>");
            }
            sb.append("</table>");
        }
        return sb.append("</td></tr></table></td></tr></table>").toString();
    }

    /**
     * Escapes text and highlights what students scan for: code (Iterable&lt;T&gt;, Student.java, main(), Java keywords)
     * in a code chip, acronyms (JVM, API) in the accent colour, numbers and Markdown **bold** in bold.
     */
    static String richText(String s) {
        String text = s == null ? "" : s.strip();
        Matcher m = HIGHLIGHT.matcher(text);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            sb.append(esc(text.substring(last, m.start())));
            if (m.group("bold") != null) sb.append("<b>").append(richText(m.group("bold"))).append("</b>");
            else if (m.group("cite") != null) sb.append(citation(m.group("cite")));
            else if (m.group("tick") != null) sb.append(code(m.group("tick")));
            else if (m.group("code") != null) sb.append(code(m.group("code")));
            else if (m.group("acro") != null) sb.append("<b><font color='").append(accentText()).append("'>").append(esc(m.group("acro"))).append("</font></b>");
            else sb.append("<b>").append(esc(m.group())).append("</b>");
            last = m.end();
        }
        sb.append(esc(text.substring(last)));
        return sb.toString().replaceAll("(<br>){2,}", "<br><br>");
    }

    private static final Pattern HIGHLIGHT = Pattern.compile(
            "\\*\\*(?<bold>.+?)\\*\\*"
            + "|\\[(?<cite>\\d{1,2}(?:\\s*,\\s*\\d{1,2})*)\\]"                                // [1], [1, 2]
            + "|`(?<tick>[^`]+)`"
            + "|(?<code>\\b[A-Z][A-Za-z0-9]*<[A-Za-z0-9?, ]{1,15}>"                          // Iterable<T>, Map<K,V>
            + "|\\b[A-Za-z_]\\w*\\.(?:java|class)\\b|(?<![\\w.])\\.class\\b"                   // Student.java, .class
            + "|\\b[a-z_]\\w*\\(\\)"                                                          // main()
            + "|\\b(?:try/catch(?:/finally)?|extends|implements|instanceof|import|null|void|boolean|byte|int|char|float)\\b"
            + "|\\b(?:short|long|double)\\b(?=,| and | or | for ))"                    // only when listed as types
            + "|(?<acro>\\b[A-Z]{2,5}s?\\b)"                                                  // JVM, APIs
            + "|\\b\\d+(?:\\.\\d+)?%?(?![\\w<])");                                            // 8, 16, 64, 3.5%

    private static String code(String s) {
        String bg = hex(Theme.tint(Theme.ACCENT, 0.14));
        return "<code style='background-color:" + bg + "; font-size:12pt'>&nbsp;" + esc(s) + "&nbsp;</code>";
    }

    private static String esc(String s) {
        return QuestionPanel.escape(s).replace("'", "&#39;").replace("\n", "<br>");
    }

    private static String attr(String s) {
        return QuestionPanel.escape(s).replace("'", "&#39;").replace("\"", "&quot;");
    }

    /** Accent for text on the page: a lighter violet in dark mode so it stays readable. */
    /** A citation marker such as [1] as a small accent tag; the brackets stay so the text still reads [1]. */
    private static String citation(String numbers) {
        return "<span style='background-color:" + hex(Theme.tint(Theme.ACCENT, 0.18)) + "; font-size:10pt'><font color='"
                + accentText() + "'><b>&nbsp;[" + esc(numbers.replaceAll("\\s+", "")) + "]&nbsp;</b></font></span>";
    }

    /**
     * Light Markdown for chat answers: paragraphs, "- " / "1. " lists, "#" headings, **bold**, `code` and [n]
     * citations, all with the same highlighting as summaries. Unknown syntax is shown as plain text.
     */
    static String markdown(String text) {
        StringBuilder sb = new StringBuilder();
        List<String[]> list = new ArrayList<>();   // {marker, item}
        StringBuilder para = new StringBuilder();
        for (String raw : (text == null ? "" : text.strip()).split("\\r?\\n")) {
            String line = raw.strip();
            Matcher bullet = Pattern.compile("^(?:[-*\u2022]|(\\d{1,2})[.)])\\s+(.*)$").matcher(line);
            Matcher heading = Pattern.compile("^#{1,4}\\s+(.*)$").matcher(line);
            if (bullet.matches()) {
                flushParagraph(sb, para);
                list.add(new String[]{bullet.group(1), bullet.group(2)});
            } else if (heading.matches()) {
                flushParagraph(sb, para);
                flushList(sb, list);
                sb.append("<div style='margin-top:6px; margin-bottom:6px; font-size:14.5pt'><b>").append(richText(heading.group(1)))
                  .append("</b></div>");
            } else if (line.isEmpty()) {
                flushParagraph(sb, para);
                flushList(sb, list);
            } else {
                flushList(sb, list);
                if (para.length() > 0) para.append(' ');
                para.append(line);
            }
        }
        flushParagraph(sb, para);
        flushList(sb, list);
        return sb.toString();
    }

    private static void flushParagraph(StringBuilder sb, StringBuilder para) {
        if (para.length() == 0) return;
        sb.append("<div style='margin-bottom:8px'>").append(richText(para.toString())).append("</div>");
        para.setLength(0);
    }

    private static void flushList(StringBuilder sb, List<String[]> items) {
        if (items.isEmpty()) return;
        sb.append("<table cellpadding='0' cellspacing='0' style='margin-bottom:8px'>");
        for (String[] item : items) {
            String marker = item[0] == null ? "<b>&#8250;</b>" : "<b>" + esc(item[0]) + ".</b>";
            sb.append("<tr><td valign='top' width='22'><font color='").append(accentText()).append("'>").append(marker)
              .append("</font></td><td valign='top' style='padding-bottom:5px'>").append(richText(item[1])).append("</td></tr>");
        }
        sb.append("</table>");
        items.clear();
    }

    static String accentText() { return Theme.isDarkColor(Theme.card()) ? "#b89cff" : hex(Theme.ACCENT); }

    /** "Lecture 1.pdf" -> "Lecture 1": the file extension adds nothing to a heading. */
    static String displayTitle(String title) {
        return title == null ? "" : title.replaceFirst("(?i)\\.(pdf|docx|txt|md|markdown)$", "");
    }

    private static String link(String href, String html) {
        return "<a href='" + href + "'><font color='" + accentText() + "'>" + html + "</font></a>";
    }

    private static String tint(double amount) { return hex(Theme.tint(Theme.ACCENT, amount)); }
    private static String muted() { return hex(Theme.muted()); }
    private static String hex(Color c) { return Theme.hex(c); }
}
