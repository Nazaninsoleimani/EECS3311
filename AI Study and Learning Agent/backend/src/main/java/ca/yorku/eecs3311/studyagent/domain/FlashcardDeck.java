package ca.yorku.eecs3311.studyagent.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlashcardDeck {
    private final String id;
    private final String courseId;
    private final String title;
    private final LocalDateTime createdAt;
    private final List<Flashcard> cards = new ArrayList<>();
    private String notice;

    public FlashcardDeck(String id, String courseId, String title) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.createdAt = LocalDateTime.now();
    }

    public void add(Flashcard card) { cards.add(card); }

    public List<Flashcard> dueCards(LocalDate today) {
        return cards.stream().filter(c -> c.isDue(today)).toList();
    }

    public Optional<Flashcard> findCard(String cardId) {
        return cards.stream().filter(c -> c.getId().equals(cardId)).findFirst();
    }

    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Flashcard> getCards() { return cards; }
    public String getNotice() { return notice; }
    public void setNotice(String notice) { this.notice = notice; }
}
