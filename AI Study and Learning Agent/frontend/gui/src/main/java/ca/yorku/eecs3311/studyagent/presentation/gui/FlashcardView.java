package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.Flashcard;
import ca.yorku.eecs3311.studyagent.domain.FlashcardRequest;
import ca.yorku.eecs3311.studyagent.domain.ReviewRating;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

/**
 * F07: generate flashcards from the material and review the due ones (Leitner boxes). The current card is drawn as
 * a real card that flips (click or Space) to show the answer; it is then rated with coloured buttons (or keys 1-4).
 */
public class FlashcardView extends BaseView {
    private final JTextField topic = new JTextField(22);
    private final JSpinner count = new JSpinner(new SpinnerNumberModel(10, 1, 50, 1));
    private final JLabel dueLabel = new JLabel(" ");
    private final CenteredText front = cardText(22f, Font.BOLD);
    private final CenteredText back = cardText(20f, Font.PLAIN);
    private final CenteredText questionEcho = cardText(13.5f, Font.PLAIN);
    private final CenteredText emptyTitle = cardText(22f, Font.BOLD);
    private final CenteredText emptyText = cardText(14f, Font.PLAIN);
    private final JLabel sideTag = new JLabel();
    private final JLabel boxLabel = new JLabel(" ");
    private final JLabel position = new JLabel(" ");
    private final JProgressBar sessionBar = new JProgressBar();
    private final JButton show = accentButton("Show answer   (Space)", this::reveal);
    private final List<JButton> ratingButtons = new ArrayList<>();
    private final List<Flashcard> due = new ArrayList<>();
    private final FlipCard card = new FlipCard();
    private final CardLayout controlsLayout = new CardLayout();
    private final JPanel controls = new JPanel(controlsLayout);
    private int sessionTotal;
    private int reviewed;
    private boolean revealed;

    public FlashcardView(GuiContext ctx) {
        super(ctx);
        topic.putClientProperty("JTextField.placeholderText", "e.g. primitive types, observer pattern");
        JPanel settingsRow = row(new JLabel("Topic"), topic, new JLabel("   Cards"), count, accentButton("Generate deck", this::requestDeck));
        settingsRow.setOpaque(false);
        Card settings = new Card(new BorderLayout());
        settings.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        settings.add(settingsRow, BorderLayout.CENTER);
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.add(heading("Flashcards"), BorderLayout.NORTH);
        top.add(settings, BorderLayout.CENTER);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Session bar: due count, position and Leitner box.
        dueLabel.setFont(dueLabel.getFont().deriveFont(Font.BOLD));
        position.setForeground(Theme.muted());
        boxLabel.setForeground(Theme.muted());
        sessionBar.setPreferredSize(new Dimension(10, 6));
        JPanel status = new JPanel(new BorderLayout(12, 0));
        status.setOpaque(false);
        status.add(dueLabel, BorderLayout.WEST);
        status.add(position, BorderLayout.CENTER);
        status.add(boxLabel, BorderLayout.EAST);
        JPanel session = new JPanel(new BorderLayout(0, 6));
        session.setOpaque(false);
        session.add(status, BorderLayout.NORTH);
        session.add(sessionBar, BorderLayout.SOUTH);

        JPanel stage = new JPanel(new GridBagLayout());
        stage.setOpaque(false);
        stage.add(card);
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(session, BorderLayout.NORTH);
        center.add(stage, BorderLayout.CENTER);

        // Controls: "Show answer" before the flip, the four ratings after it.
        JPanel showRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        showRow.setOpaque(false);
        show.setFont(show.getFont().deriveFont(Font.BOLD, 14.5f));
        show.setPreferredSize(new Dimension(260, 42));
        showRow.add(show);
        JPanel rateRow = new JPanel(new GridLayout(1, 4, 10, 0));
        rateRow.setOpaque(false);
        String[][] meta = {{"Again", "forgot it", "1"}, {"Hard", "struggled", "2"}, {"Good", "got it", "3"}, {"Easy", "too easy", "4"}};
        Color[] tones = {Theme.DANGER, Theme.WARNING, Theme.SUCCESS, new Color(0x2563EB)};
        ReviewRating[] ratings = ReviewRating.values();
        for (int i = 0; i < ratings.length; i++) {
            ReviewRating r = ratings[i];
            JButton b = new JButton("<html><center><b>" + meta[i][0] + "</b><br><span style='font-size:9px'>" + meta[i][1]
                    + " &middot; " + meta[i][2] + "</span></center></html>");
            b.addActionListener(e -> rateCurrent(r));
            b.setBackground(tones[i]);
            b.setForeground(Color.WHITE);
            b.setPreferredSize(new Dimension(130, 48));
            b.setFocusable(false);
            ratingButtons.add(b);
            rateRow.add(b);
        }
        JPanel rateWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        rateWrap.setOpaque(false);
        rateWrap.add(rateRow);
        controls.setOpaque(false);
        controls.add(showRow, "show");
        controls.add(rateWrap, "rate");
        JLabel keys = hint("Space flips the card  ·  1 Again  2 Hard  3 Good  4 Easy");
        keys.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.add(controls, BorderLayout.CENTER);
        bottom.add(keys, BorderLayout.SOUTH);
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        installKeys();
    }

    private static CenteredText cardText(float size, int style) {
        return new CenteredText(size, style);
    }

    /**
     * Read-only text whose every line is centred and which wraps to its container's width (a JLabel with HTML
     * only centres a fixed-width block, which leaves the text off-centre).
     */
    static final class CenteredText extends JTextPane {
        CenteredText(float size, int style) {
            setEditable(false);
            setFocusable(false);
            setOpaque(false);
            setBorder(null);
            setHighlighter(null);
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
            setFont(new JLabel().getFont().deriveFont(style, size));
            setText(" ");
        }

        @Override
        public void setText(String t) {
            super.setText(t);
            SimpleAttributeSet centre = new SimpleAttributeSet();
            StyleConstants.setAlignment(centre, StyleConstants.ALIGN_CENTER);
            getStyledDocument().setParagraphAttributes(0, getDocument().getLength() + 1, centre, false);
        }

        /** Height for the width the parent gives us, so the text wraps instead of widening the card. */
        @Override
        public Dimension getPreferredSize() {
            int w = getParent() != null && getParent().getWidth() > 0 ? getParent().getWidth() : 560;
            View root = getUI().getRootView(this);
            root.setSize(w, Integer.MAX_VALUE);
            return new Dimension(w, (int) Math.ceil(root.getPreferredSpan(View.Y_AXIS)));
        }
    }

    /** Centres a component vertically and lets it use the full width. */
    private static JPanel middle(JComponent c) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        p.add(c, g);
        return p;
    }

    private void installKeys() {
        bind("SPACE", () -> { if (revealed) return; reveal(); });
        for (int i = 0; i < 4; i++) {
            ReviewRating r = ReviewRating.values()[i];
            bind(String.valueOf(i + 1), () -> { if (revealed) rateCurrent(r); });
        }
    }

    private void bind(String key, Runnable action) {
        card.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key.length() == 1 ? "typed " + key : key), key);
        card.getActionMap().put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    @Override public String title() { return "Flashcards"; }

    @Override
    public void render() {
        Course c = ctx.course();
        due.clear();
        if (c != null) due.addAll(facade.dueFlashcards(c.getId()));
        sessionTotal = due.size();
        reviewed = 0;
        showNext();
    }

    public void requestDeck() {
        Course c = requireCourse();
        if (c == null) return;
        if (topic.getText().isBlank()) {
            showError("Enter a topic for the cards, e.g. \"observer pattern\".");
            return;
        }
        FlashcardRequest request;
        try {
            request = new FlashcardRequest(ctx.studentId, c.getId(), List.of(), topic.getText(), (Integer) count.getValue());
        } catch (ValidationException e) {
            showError(e.getMessage());
            return;
        }
        runAsync("Writing flashcards (AI)", () -> facade.generateFlashcards(request), deck -> {
            ctx.status(deck.getCards().size() + " card(s) added" + (deck.getNotice() == null ? "" : ". " + deck.getNotice()));
            render();
        });
    }

    public void rateCard(String cardId, ReviewRating rating) {
        attempt(() -> {
            Flashcard updated = facade.reviewFlashcard(cardId, rating);
            ctx.status("Next review of this card: " + updated.getNextReview());
            due.removeIf(f -> f.getId().equals(cardId));
            reviewed++;
            showNext();
            ctx.fireDataChanged();
        });
    }

    private void rateCurrent(ReviewRating rating) {
        if (!due.isEmpty()) rateCard(due.get(0).getId(), rating);
    }

    private void reveal() {
        if (due.isEmpty() || revealed) return;
        revealed = true;
        back.setText(html(due.get(0).getBack(), 560));
        show.setEnabled(false);
        ratingButtons.forEach(b -> b.setEnabled(true));
        controlsLayout.show(controls, "rate");
        card.flip(true);
    }

    private void showNext() {
        revealed = false;
        dueLabel.setText(due.size() + " card(s) due today");
        ratingButtons.forEach(b -> b.setEnabled(false));
        back.setText(" ");
        controlsLayout.show(controls, "show");
        sessionBar.setMaximum(Math.max(1, sessionTotal));
        sessionBar.setValue(reviewed);
        sessionBar.setVisible(sessionTotal > 0);
        controls.setVisible(!due.isEmpty());
        if (due.isEmpty()) {
            position.setText(reviewed > 0 ? reviewed + " reviewed this session" : "");
            boxLabel.setText(" ");
            String title = ctx.course() == null ? "Select a course first." : reviewed > 0 ? "All caught up!" : "No cards due";
            front.setText(html(title, 560));
            emptyTitle.setText(html(title, 560));
            emptyText.setText(ctx.course() == null ? " " : html(reviewed > 0
                    ? "Great work. Cards come back when they are due, so the ones you found hard return sooner."
                    : "Type a topic above and press Generate deck, or come back tomorrow.", 520));
            show.setEnabled(false);
            card.showEmpty(reviewed > 0);
            return;
        }
        Flashcard f = due.get(0);
        position.setText("Card " + (reviewed + 1) + " of " + sessionTotal);
        boxLabel.setText(boxDots(f.getBox()));
        front.setText(html(f.getFront(), 560));
        questionEcho.setText(html(f.getFront(), 560));
        show.setEnabled(true);
        card.flip(false);
        card.requestFocusInWindow();
    }

    /** "Level ●●○○○" for Leitner box 1..5: how well this card is already known. */
    private static String boxDots(int box) {
        int b = Math.max(1, Math.min(Flashcard.MAX_BOX, box));
        return "<html>Memory level &nbsp;<font color='" + Theme.hex(Theme.ACCENT) + "'>" + "●".repeat(b) + "</font><font color='"
                + Theme.hex(Theme.border()) + "'>" + "●".repeat(Flashcard.MAX_BOX - b) + "</font></html>";
    }

    private static String html(String s, int width) {
        return s;   // plain text: CenteredText centres and wraps it
    }

    /** The flashcard: a rounded card with a coloured top stripe that flips between question and answer. */
    private final class FlipCard extends JPanel {
        private final CardLayout faces = new CardLayout();
        private final JPanel content = new JPanel(faces);
        private double scaleX = 1;
        private Color stripe = Theme.ACCENT;

        FlipCard() {
            super(new BorderLayout());
            setOpaque(false);
            setFocusable(true);
            setBorder(BorderFactory.createEmptyBorder(26, 36, 30, 36));
            setPreferredSize(new Dimension(720, 340));
            content.setOpaque(false);

            JPanel frontFace = face(sideTagFor("QUESTION", Theme.ACCENT), front, hintLabel("Click the card to see the answer"));
            JPanel backFace = new JPanel(new BorderLayout(0, 12));
            backFace.setOpaque(false);
            questionEcho.setForeground(Theme.muted());
            JPanel backTop = new JPanel(new BorderLayout(0, 8));
            backTop.setOpaque(false);
            backTop.add(sideTagFor("ANSWER", Theme.SUCCESS), BorderLayout.NORTH);
            backTop.add(questionEcho, BorderLayout.CENTER);
            backFace.add(backTop, BorderLayout.NORTH);
            backFace.add(middle(back), BorderLayout.CENTER);
            backFace.add(hintLabel("How well did you know it?"), BorderLayout.SOUTH);
            JPanel emptyFace = new JPanel(new BorderLayout(0, 10));
            emptyFace.setOpaque(false);
            emptyFace.add(sideTag, BorderLayout.NORTH);
            JLabel big = new JLabel("✓", SwingConstants.CENTER);
            big.setFont(big.getFont().deriveFont(Font.BOLD, 40f));
            big.setForeground(Theme.SUCCESS);
            JPanel mid = new JPanel(new BorderLayout(0, 6));
            mid.setOpaque(false);
            mid.add(big, BorderLayout.NORTH);
            mid.add(emptyTitle, BorderLayout.CENTER);
            emptyText.setForeground(Theme.muted());
            mid.add(emptyText, BorderLayout.SOUTH);
            emptyFace.add(mid, BorderLayout.CENTER);

            content.add(frontFace, "front");
            content.add(backFace, "back");
            content.add(emptyFace, "empty");
            add(content, BorderLayout.CENTER);

            MouseAdapter flipOnClick = new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    requestFocusInWindow();
                    if (!revealed && !due.isEmpty()) reveal();
                }
            };
            addMouseListener(flipOnClick);
            for (JComponent c : List.of(front, back, questionEcho)) {   // text panes would otherwise swallow the click
                c.addMouseListener(flipOnClick);
                c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        private JPanel face(JComponent tag, JComponent text, JComponent hint) {
            JPanel p = new JPanel(new BorderLayout(0, 10));
            p.setOpaque(false);
            p.add(tag, BorderLayout.NORTH);
            p.add(middle(text), BorderLayout.CENTER);
            p.add(hint, BorderLayout.SOUTH);
            return p;
        }

        private JComponent sideTagFor(String text, Color c) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            p.setOpaque(false);
            Card.Pill pill = new Card.Pill(text, c);
            pill.setFont(pill.getFont().deriveFont(Font.BOLD, 11.5f));
            p.add(pill);
            return p;
        }

        private JLabel hintLabel(String s) {
            JLabel l = new JLabel(s, SwingConstants.CENTER);
            l.setForeground(Theme.muted());
            l.setFont(l.getFont().deriveFont(12f));
            return l;
        }

        /** Shows the question (false) or the answer (true), turning the card over. */
        void flip(boolean toBack) {
            Color target = toBack ? Theme.SUCCESS : Theme.ACCENT;
            String face = toBack ? "back" : "front";
            if (!toBack) {   // a new card simply appears
                stripe = target;
                faces.show(content, face);
                scaleX = 1;
                repaint();
                return;
            }
            Animations.animate(260, t -> {
                scaleX = Math.abs(1 - 2 * t);
                if (t >= 0.5 && stripe != target) {
                    stripe = target;
                    faces.show(content, face);
                }
                repaint();
            }, () -> {
                scaleX = 1;
                stripe = target;
                faces.show(content, face);
                repaint();
            });
        }

        void showEmpty(boolean celebrate) {
            sideTag.setText("");
            stripe = celebrate ? Theme.SUCCESS : Theme.border();
            faces.show(content, "empty");
            scaleX = 1;
            repaint();
        }

        @Override
        public void paint(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            if (scaleX < 0.999) {   // the flip: squeeze horizontally around the centre
                g.translate(getWidth() / 2.0, 0);
                g.scale(Math.max(0.02, scaleX), 1);
                g.translate(-getWidth() / 2.0, 0);
            }
            super.paint(g);
            g.dispose();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 8, h = getHeight() - 8;
            for (int i = 4; i >= 1; i--) {   // soft shadow
                g.setColor(Theme.withAlpha(Color.BLACK, 10));
                g.fillRoundRect(4 - i + 2, 4 - i + 5, w + 2 * i - 4, h + 2 * i - 4, 26, 26);
            }
            g.setColor(Theme.card());
            g.fillRoundRect(4, 4, w, h, 24, 24);
            g.setClip(4, 4, w, 8);
            g.setColor(stripe);
            g.fillRoundRect(4, 4, w, 24, 24, 24);
            g.setClip(null);
            g.setColor(Theme.border());
            g.drawRoundRect(4, 4, w, h, 24, 24);
            g.dispose();
        }
    }

    CenteredText frontLabel() { return front; }
    JLabel dueLabel() { return dueLabel; }
    void revealForTest() { reveal(); }
    CenteredText backLabel() { return back; }
}
