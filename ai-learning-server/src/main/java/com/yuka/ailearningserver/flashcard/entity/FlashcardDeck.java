package com.yuka.ailearningserver.flashcard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuka.ailearningserver.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * A deck of flashcards. Card/due counts are derived from {@code flashcards}.
 */
@Getter
@Setter
@TableName("flashcard_decks")
public class FlashcardDeck extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Logical FK → users.id. */
    private Long userId;

    /** Logical FK → subjects.id; null for subject-independent decks. */
    private Long subjectId;

    private String name;

    private String description;
}
