package com.yuka.ailearningserver.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoteRequest(
        @NotBlank @Size(max = 255) String title,
        String content,
        Boolean pinned) {
}
