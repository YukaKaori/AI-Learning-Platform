package com.yuka.ailearningserver.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code subjectId} is optional and must reference a subject owned by the caller. */
public record CreateNoteRequest(
        @NotBlank @Size(max = 255) String title,
        String content,
        Boolean pinned,
        String subjectId) {
}
