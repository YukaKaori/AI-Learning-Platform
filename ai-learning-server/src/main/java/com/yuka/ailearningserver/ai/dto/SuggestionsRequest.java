package com.yuka.ailearningserver.ai.dto;

import jakarta.validation.constraints.Size;

public record SuggestionsRequest(
        @Size(max = 64) String subjectName,
        @Size(max = 500) String subjectDescription) {
}
