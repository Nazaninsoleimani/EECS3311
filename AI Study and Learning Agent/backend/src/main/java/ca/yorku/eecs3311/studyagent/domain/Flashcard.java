package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDate;

/** Leitner spaced repetition: Good/Easy promote one box, Again/Hard return to box 1. */
public class Flashcard {
    public static final int MAX_BOX = 5;
    private static final int[] INTERVAL_DAYS = {1, 2, 4, 8, 16};

    private final String id;
    private final String deckId;
    private final String conceptId;
    private final String front;
    private final String back;
    private int box = 1;
    private LocalDate nextReview;

    public Flashcard(String id, String deckId, String conceptId, String front, String back) {
        if (front == null || front.isBlank() || back == null || back.isBlank()) {
            throw new ValidationException("Flashcard needs both a front and a back");
        }
        this.id = id;
        this.deckId = deckId;
        this.conceptId = conceptId;
        this.front = front.trim();
        this.back = back.trim();
        this.nextReview = LocalDate.now();
    }

    public void recordReview(ReviewRating rating) {
        recordReview(rating, LocalDate.now());
    }

    public void recordReview(ReviewRating rating, LocalDate today) {
        box = switch (rating) {
            case GOOD, EASY -> Math.min(MAX_BOX, box + 1);
            case AGAIN, HARD -> 1;
        };
        nextReview = today.plusDays(INTERVAL_DAYS[box - 1]);
    }

    public boolean isDue(LocalDate today) {
        return !nextReview.isAfter(today);
    }

    /** Converts a review rating into mastery evidence (F07 -> F13). */
    public GradeResult toGradeResult(ReviewRating rating) {
        double score = switch (rating) {
            case AGAIN -> 0.0;
            case HARD -> 0.4;
            case GOOD -> 0.8;
            case EASY -> 1.0;
        };
        return new GradeResult(id, conceptId, score >= 0.8, score, "Flashcard review: " + rating);
    }

    public String getId() { return id; }
    public String getDeckId() { return deckId; }
    public String getConceptId() { return conceptId; }
    public String getFront() { return front; }
    public String getBack() { return back; }
    public int getBox() { return box; }
    public LocalDate getNextReview() { return nextReview; }
}
