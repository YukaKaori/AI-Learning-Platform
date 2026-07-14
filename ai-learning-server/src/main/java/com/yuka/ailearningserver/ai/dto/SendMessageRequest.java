package com.yuka.ailearningserver.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code subjectId} is the preferred subject context: it must reference a
 * subject owned by the caller, is persisted on the conversation, and lets the
 * server resolve name/description/materials itself. Per the partial-update
 * convention, null means "keep the conversation's current link" and {@code ""}
 * unlinks it. {@code subjectName}/{@code subjectDescription} remain as
 * plain-text hints for clients that don't send an id (see
 * {@code ContextHints}) — they never override a resolved subject.
 */
public record SendMessageRequest(
        @NotBlank @Size(max = 8000) String content,
        @Size(max = 64) String subjectName,
        @Size(max = 500) String subjectDescription,
        String subjectId) {
}
