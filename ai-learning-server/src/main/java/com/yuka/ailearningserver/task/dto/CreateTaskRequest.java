package com.yuka.ailearningserver.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * New tasks always start as {@code todo}. {@code priority} defaults to
 * {@code medium} when absent; {@code dueAt} is epoch milliseconds (null =
 * unscheduled backlog); {@code subjectId} must reference a subject owned by
 * the caller.
 */
public record CreateTaskRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 500) String description,
        String priority,
        Long dueAt,
        String subjectId) {
}
