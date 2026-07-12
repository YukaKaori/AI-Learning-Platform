package com.yuka.ailearningserver.flashcard.dto;

import jakarta.validation.constraints.Size;

public record UpdateDeckRequest(
        @Size(max = 128) String name,
        @Size(max = 500) String description) {
}
