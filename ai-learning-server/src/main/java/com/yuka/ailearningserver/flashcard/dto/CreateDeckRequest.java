package com.yuka.ailearningserver.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code subjectId} is optional and must reference a subject owned by the caller. */
public record CreateDeckRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 500) String description,
        String subjectId) {
}
