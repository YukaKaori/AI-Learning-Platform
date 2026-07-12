package com.yuka.ailearningserver.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SummaryRequest(
        @NotBlank @Size(max = 12000) String text,
        @Size(max = 64) String subjectName,
        @Size(max = 500) String subjectDescription) {
}
