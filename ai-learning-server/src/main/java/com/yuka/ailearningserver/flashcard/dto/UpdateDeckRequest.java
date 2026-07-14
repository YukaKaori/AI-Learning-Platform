package com.yuka.ailearningserver.flashcard.dto;

import jakarta.validation.constraints.Size;

/**
 * Partial update — only non-null fields are applied. {@code subjectId = ""}
 * unlinks the deck from its subject.
 */
public record UpdateDeckRequest(
        @Size(max = 128) String name,
        @Size(max = 500) String description,
        String subjectId) {
}
