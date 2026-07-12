package com.yuka.ailearningserver.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExplainRequest(
        @NotBlank @Size(max = 2000) String topic,
        @Size(max = 64) String subjectName,
        @Size(max = 500) String subjectDescription) {
}
