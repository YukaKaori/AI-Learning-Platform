package com.yuka.ailearningserver.note.dto;

import jakarta.validation.constraints.Size;

/**
 * Partial update — only non-null fields are applied. {@code subjectId = ""}
 * unlinks the note from its subject.
 */
public record UpdateNoteRequest(
        @Size(max = 255) String title,
        String content,
        Boolean pinned,
        String subjectId) {
}
