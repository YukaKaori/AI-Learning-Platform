package com.yuka.ailearningserver.calendar.dto;

import jakarta.validation.constraints.Size;

/**
 * Partial update — only non-null fields are applied. Nullable columns use an
 * explicit clear sentinel: {@code title = ""} removes the label and
 * {@code subjectId = ""} unlinks the subject. The resulting time range is
 * re-validated ({@code endsAt > startsAt}) whenever either bound changes.
 */
public record UpdateStudySessionRequest(
        @Size(max = 255) String title,
        String subjectId,
        Long startsAt,
        Long endsAt) {
}
