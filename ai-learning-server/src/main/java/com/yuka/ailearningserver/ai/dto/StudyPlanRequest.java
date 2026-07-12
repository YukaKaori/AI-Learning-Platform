package com.yuka.ailearningserver.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StudyPlanRequest(
        @NotBlank @Size(max = 500) String goal,
        @Min(1) int availableMinutesPerDay,
        List<String> subjects) {
}
