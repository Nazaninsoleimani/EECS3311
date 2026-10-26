package ca.yorku.eecs3311.studyagent.persistence;

import ca.yorku.eecs3311.studyagent.domain.*;
import java.util.List;
import java.util.Optional;

public interface FlashcardRepository {
    void saveDeck(FlashcardDeck deck);
    Optional<FlashcardDeck> findDeck(String deckId);
    List<FlashcardDeck> findDecksByCourse(String courseId);
    /** Finds the deck that contains the card. */
    Optional<FlashcardDeck> findDeckByCard(String cardId);
    void deleteByCourse(String courseId);
}
