package com.yuka.ailearningserver.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 16) String color,
        @Size(max = 32) String icon,
        @Size(max = 500) String description) {
}
