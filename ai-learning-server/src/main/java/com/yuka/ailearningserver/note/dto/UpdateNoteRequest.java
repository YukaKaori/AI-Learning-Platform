package com.yuka.ailearningserver.note.dto;

import jakarta.validation.constraints.Size;

/** Partial update — only non-null fields are applied. */
public record UpdateNoteRequest(
        @Size(max = 255) String title,
        String content,
        Boolean pinned) {
}
