package com.yuka.ailearningserver.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoteActionRequest(
        @NotNull NoteAction action,
        @NotBlank @Size(max = 12000) String text,
        @Size(max = 64) String subjectName,
        @Size(max = 500) String subjectDescription) {
}
