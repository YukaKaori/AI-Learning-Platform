package com.yuka.ailearningserver.ai.dto;

import jakarta.validation.constraints.Size;

public record FlashcardGenerationRequest(
        @Size(max = 12000) String text,
        @Size(max = 64) String subjectName,
        @Size(max = 500) String subjectDescription,
        @Size(max = 128) String deckName,
        @Size(max = 500) String deckDescription) {
}
