package com.yuka.ailearningserver.flashcard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuka.ailearningserver.common.OwnershipGuard;
import com.yuka.ailearningserver.flashcard.dto.CardResponse;
import com.yuka.ailearningserver.flashcard.dto.CreateCardRequest;
import com.yuka.ailearningserver.flashcard.dto.CreateDeckRequest;
import com.yuka.ailearningserver.flashcard.dto.DeckResponse;
import com.yuka.ailearningserver.flashcard.dto.UpdateCardRequest;
import com.yuka.ailearningserver.flashcard.dto.UpdateDeckRequest;
import com.yuka.ailearningserver.flashcard.entity.Flashcard;
import com.yuka.ailearningserver.flashcard.entity.FlashcardDeck;
import com.yuka.ailearningserver.flashcard.mapper.FlashcardDeckMapper;
import com.yuka.ailearningserver.flashcard.mapper.FlashcardMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FlashcardService {

    private final FlashcardDeckMapper deckMapper;
    private final FlashcardMapper cardMapper;

    public FlashcardService(FlashcardDeckMapper deckMapper, FlashcardMapper cardMapper) {
        this.deckMapper = deckMapper;
        this.cardMapper = cardMapper;
    }

    public List<DeckResponse> listDecks(Long userId) {
        return deckMapper.selectList(new LambdaQueryWrapper<FlashcardDeck>()
                        .eq(FlashcardDeck::getUserId, userId)
                        .orderByDesc(FlashcardDeck::getUpdatedAt))
                .stream()
                .map(deck -> toDeckResponse(userId, deck))
                .toList();
    }

    public DeckResponse getDeck(Long userId, Long deckId) {
        return toDeckResponse(userId, requireOwnedDeck(userId, deckId));
    }

    public DeckResponse createDeck(Long userId, CreateDeckRequest request) {
        FlashcardDeck deck = new FlashcardDeck();
        deck.setUserId(userId);
        deck.setName(request.name());
        deck.setDescription(request.description());
        deckMapper.insert(deck);
        return toDeckResponse(userId, deck);
    }

    public DeckResponse updateDeck(Long userId, Long deckId, UpdateDeckRequest request) {
        FlashcardDeck deck = requireOwnedDeck(userId, deckId);
        if (request.name() != null && !request.name().isBlank()) {
            deck.setName(request.name());
        }
        if (request.description() != null) {
            deck.setDescription(request.description());
        }
        deckMapper.updateById(deck);
        return toDeckResponse(userId, deck);
    }

    public void deleteDeck(Long userId, Long deckId) {
        FlashcardDeck deck = requireOwnedDeck(userId, deckId);
        cardMapper.delete(new LambdaQueryWrapper<Flashcard>().eq(Flashcard::getDeckId, deck.getId()));
        deckMapper.deleteById(deck.getId());
    }

    public List<CardResponse> listCards(Long userId, Long deckId) {
        requireOwnedDeck(userId, deckId);
        return cardMapper.selectList(new LambdaQueryWrapper<Flashcard>()
                        .eq(Flashcard::getDeckId, deckId)
                        .orderByAsc(Flashcard::getCreatedAt))
                .stream()
                .map(CardResponse::from)
                .toList();
    }

    public CardResponse createCard(Long userId, Long deckId, CreateCardRequest request) {
        requireOwnedDeck(userId, deckId);
        Flashcard card = new Flashcard();
        card.setDeckId(deckId);
        card.setUserId(userId);
        card.setFront(request.front());
        card.setBack(request.back());
        card.setReviewCount(0);
        cardMapper.insert(card);
        return CardResponse.from(card);
    }

    public CardResponse updateCard(Long userId, Long cardId, UpdateCardRequest request) {
        Flashcard card = requireOwnedCard(userId, cardId);
        if (request.front() != null && !request.front().isBlank()) {
            card.setFront(request.front());
        }
        if (request.back() != null && !request.back().isBlank()) {
            card.setBack(request.back());
        }
        cardMapper.updateById(card);
        return CardResponse.from(card);
    }

    public void deleteCard(Long userId, Long cardId) {
        Flashcard card = requireOwnedCard(userId, cardId);
        cardMapper.deleteById(card.getId());
    }

    /** Used by AiGenerationService to persist an AI-generated deck in one call. */
    public DeckResponse createDeckFromGenerated(Long userId, String name, String description,
                                                 List<CreateCardRequest> cards) {
        DeckResponse deck = createDeck(userId, new CreateDeckRequest(name, description));
        Long deckId = Long.valueOf(deck.id());
        for (CreateCardRequest card : cards) {
            createCard(userId, deckId, card);
        }
        return getDeck(userId, deckId);
    }

    private DeckResponse toDeckResponse(Long userId, FlashcardDeck deck) {
        long cardCount = cardMapper.selectCount(new LambdaQueryWrapper<Flashcard>()
                .eq(Flashcard::getDeckId, deck.getId()));
        long dueCount = cardMapper.selectCount(new LambdaQueryWrapper<Flashcard>()
                .eq(Flashcard::getDeckId, deck.getId())
                .isNotNull(Flashcard::getDueAt)
                .le(Flashcard::getDueAt, LocalDateTime.now()));
        return DeckResponse.from(deck, (int) cardCount, (int) dueCount);
    }

    private FlashcardDeck requireOwnedDeck(Long userId, Long deckId) {
        return OwnershipGuard.require(deckMapper.selectById(deckId), FlashcardDeck::getUserId, userId,
                FlashcardErrorCode.DECK_NOT_FOUND, FlashcardErrorCode.DECK_ACCESS_DENIED);
    }

    private Flashcard requireOwnedCard(Long userId, Long cardId) {
        return OwnershipGuard.require(cardMapper.selectById(cardId), Flashcard::getUserId, userId,
                FlashcardErrorCode.CARD_NOT_FOUND, FlashcardErrorCode.CARD_ACCESS_DENIED);
    }
}
