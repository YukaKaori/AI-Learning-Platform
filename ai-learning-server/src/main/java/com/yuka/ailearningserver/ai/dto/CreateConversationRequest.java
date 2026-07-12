package com.yuka.ailearningserver.ai.dto;

import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
        @Size(max = 255) String title,
        @Size(max = 64) String subjectName) {
}
