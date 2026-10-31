package ca.yorku.eecs3311.studyagent.presentation.gui;

import ca.yorku.eecs3311.studyagent.domain.Course;
import ca.yorku.eecs3311.studyagent.domain.Flashcard;
import ca.yorku.eecs3311.studyagent.domain.FlashcardRequest;
import ca.yorku.eecs3311.studyagent.domain.ReviewRating;
import ca.yorku.eecs3311.studyagent.domain.ValidationException;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

/** F07: generate flashcards from the material and review the due ones (Leitner boxes). */
public class FlashcardView extends BaseView {
    private final JTextField topic = new JTextField(22);
    private final JSpinner count = new JSpinner(new SpinnerNumberModel(10, 1, 50, 1));
    private final JLabel dueLabel = new JLabel(" ");
    private final JLabel front = cardLabel();
    private final JLabel back = cardLabel();
    private final JButton show = button("Show answer", this::reveal);
    private final List<JButton> ratingButtons = new ArrayList<>();
    private final List<Flashcard> due = new ArrayList<>();

    public FlashcardView(GuiContext ctx) {
        super(ctx);
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading("Flashcards"), BorderLayout.NORTH);
        top.add(row(new JLabel("Topic"), topic, new JLabel("Cards"), count, button("Generate deck", this::requestDeck), dueLabel),
                BorderLayout.SOUTH);
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 10));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        card.add(front);
        card.add(back);
        JPanel buttons = row(show);
        for (ReviewRating r : ReviewRating.values()) {
            JButton b = button(r.name().charAt(0) + r.name().substring(1).toLowerCase(), () -> rateCurrent(r));
            ratingButtons.add(b);
            buttons.add(b);
        }
        add(top, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private static JLabel cardLabel() {
        JLabel l = new JLabel(" ", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 18f));
        return l;
    }

    @Override public String title() { return "Flashcards"; }

    @Override
    public void render() {
        Course c = ctx.course();
        due.clear();
        if (c != null) due.addAll(facade.dueFlashcards(c.getId()));
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
            showNext();
            ctx.fireDataChanged();
        });
    }

    private void rateCurrent(ReviewRating rating) {
        if (!due.isEmpty()) rateCard(due.get(0).getId(), rating);
    }

    private void reveal() {
        if (due.isEmpty()) return;
        back.setText(html(due.get(0).getBack()));
        show.setEnabled(false);
        ratingButtons.forEach(b -> b.setEnabled(true));
    }

    private void showNext() {
        dueLabel.setText("   " + due.size() + " card(s) due today");
        ratingButtons.forEach(b -> b.setEnabled(false));
        back.setText(" ");
        if (due.isEmpty()) {
            front.setText(ctx.course() == null ? "Select a course first." : "No cards due. Generate a deck or come back tomorrow.");
            show.setEnabled(false);
            return;
        }
        front.setText(html(due.get(0).getFront()));
        show.setEnabled(true);
    }

    private static String html(String s) {
        return "<html><div style='text-align:center'>" + QuestionPanel.escape(s) + "</div></html>";
    }

    JLabel frontLabel() { return front; }
    JLabel dueLabel() { return dueLabel; }
    void revealForTest() { reveal(); }
    JLabel backLabel() { return back; }
}
