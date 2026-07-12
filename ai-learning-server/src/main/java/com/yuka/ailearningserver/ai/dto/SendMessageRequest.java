package com.yuka.ailearningserver.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code subjectName}/{@code subjectDescription} are optional, client-supplied
 * context (see {@code ContextHints}) — sent when the chat was opened from a
 * Subject page, since subjects aren't a real server-side entity yet.
 */
public record SendMessageRequest(
        @NotBlank @Size(max = 8000) String content,
        @Size(max = 64) String subjectName,
        @Size(max = 500) String subjectDescription) {
}
