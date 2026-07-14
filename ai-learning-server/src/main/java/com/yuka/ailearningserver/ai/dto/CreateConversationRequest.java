package com.yuka.ailearningserver.ai.dto;

import jakarta.validation.constraints.Size;

/**
 * {@code subjectId} is optional and must reference a subject owned by the
 * caller; when present it is persisted on the conversation and the
 * {@code subjectName} display snapshot is taken from the real subject,
 * ignoring the plain-text {@code subjectName} field (kept for clients without
 * a subject id).
 */
public record CreateConversationRequest(
        @Size(max = 255) String title,
        @Size(max = 64) String subjectName,
        String subjectId) {
}
