package com.yuka.ailearningserver.flashcard.dto;

import com.yuka.ailearningserver.flashcard.entity.FlashcardDeck;

public record DeckResponse(String id, String subjectId, String name, String description, int cardCount,
                            int dueCount) {

    public static DeckResponse from(FlashcardDeck deck, int cardCount, int dueCount) {
        return new DeckResponse(
                String.valueOf(deck.getId()),
                deck.getSubjectId() != null ? String.valueOf(deck.getSubjectId()) : null,
                deck.getName(),
                deck.getDescription(),
                cardCount,
                dueCount);
    }
}
