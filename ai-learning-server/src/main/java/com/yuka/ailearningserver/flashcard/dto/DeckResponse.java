package com.yuka.ailearningserver.flashcard.dto;

import com.yuka.ailearningserver.flashcard.entity.FlashcardDeck;

public record DeckResponse(String id, String subjectId, String name, String description, int cardCount,
                            int dueCount, int newCount) {

    public static DeckResponse from(FlashcardDeck deck, int cardCount, int dueCount, int newCount) {
        return new DeckResponse(
                String.valueOf(deck.getId()),
                deck.getSubjectId() != null ? String.valueOf(deck.getSubjectId()) : null,
                deck.getName(),
                deck.getDescription(),
                cardCount,
                dueCount,
                newCount);
    }
}
