package ca.yorku.eecs3311.studyagent.persistence.sqlite;

import ca.yorku.eecs3311.studyagent.domain.FlashcardDeck;
import ca.yorku.eecs3311.studyagent.persistence.FlashcardRepository;
import java.util.List;
import java.util.Optional;

public class SqliteFlashcardRepository implements FlashcardRepository {
    private final SqliteDatabase db;

    public SqliteFlashcardRepository(SqliteDatabase db) { this.db = db; }

    @Override public void saveDeck(FlashcardDeck deck) { db.put("deck", deck.getId(), deck.getCourseId(), null, deck); }
    @Override public Optional<FlashcardDeck> findDeck(String deckId) { return db.get("deck", deckId, FlashcardDeck.class); }
    @Override public List<FlashcardDeck> findDecksByCourse(String courseId) { return db.byParent("deck", courseId, FlashcardDeck.class); }

    @Override
    public Optional<FlashcardDeck> findDeckByCard(String cardId) {
        return db.all("deck", FlashcardDeck.class).stream()
                .filter(d -> d.findCard(cardId).isPresent()).findFirst();
    }

    @Override public void deleteByCourse(String courseId) { db.deleteByParent("deck", courseId); }
}
