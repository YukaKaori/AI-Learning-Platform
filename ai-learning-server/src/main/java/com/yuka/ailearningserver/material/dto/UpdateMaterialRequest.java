package com.yuka.ailearningserver.material.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMaterialRequest(
        @Size(max = 255) String title,
        @Pattern(regexp = "pdf|markdown|video|article|link|document") String type,
        @Size(max = 500) String description,
        @Size(max = 1024) String sourceUrl) {
}
