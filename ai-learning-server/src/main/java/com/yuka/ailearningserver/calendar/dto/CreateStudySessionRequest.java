package com.yuka.ailearningserver.calendar.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code startsAt}/{@code endsAt} are epoch milliseconds and must satisfy
 * {@code endsAt > startsAt}; {@code subjectId} must reference a subject owned
 * by the caller.
 */
public record CreateStudySessionRequest(
        @Size(max = 255) String title,
        String subjectId,
        @NotNull Long startsAt,
        @NotNull Long endsAt) {
}
