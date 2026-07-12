package com.yuka.ailearningserver.flashcard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuka.ailearningserver.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * A single card. Front/back are markdown. The scheduling fields are reserved
 * for the spaced-repetition engine — persisted from day one so review history
 * survives the engine's arrival.
 */
@Getter
@Setter
@TableName("flashcards")
public class Flashcard extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Logical FK → flashcard_decks.id. */
    private Long deckId;

    /** Logical FK → users.id — denormalized for per-user queries without a join. */
    private Long userId;

    /** Question / prompt side (markdown). */
    private String front;

    /** Answer side (markdown). */
    private String back;

    /** When the card is next due for review; null = never reviewed (new card). */
    private LocalDateTime dueAt;

    /** Current inter-review interval in days (spaced-repetition state). */
    private Integer intervalDays;

    /** Ease factor ×1000 (SM-2 style, e.g. 2500 = 2.5) — integer to avoid float drift. */
    private Integer ease;

    private Integer reviewCount;

    private LocalDateTime lastReviewedAt;
}
