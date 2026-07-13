package com.yuka.ailearningserver.subject.dto;

import com.yuka.ailearningserver.subject.entity.Subject;

import java.time.ZoneId;

/**
 * Subject with the derived numbers the product surfaces alongside it.
 * {@code color} is an opaque accent token name resolved by the frontend
 * (e.g. {@code indigo}), never interpreted server-side.
 */
public record SubjectResponse(String id, String name, String color, String icon, String description,
                              String status, int progress, int materialCount, int noteCount, int deckCount,
                              long studyMinutes, Long lastStudiedAt, long createdAt, long updatedAt) {

    /** Derived per-subject aggregates computed by the service, zero-valued when absent. */
    public record Derived(int materialCount, int noteCount, int deckCount, long studyMinutes, Long lastStudiedAt) {

        public static final Derived NONE = new Derived(0, 0, 0, 0, null);
    }

    public static SubjectResponse from(Subject subject, Derived derived) {
        return new SubjectResponse(
                String.valueOf(subject.getId()),
                subject.getName(),
                subject.getColor(),
                subject.getIcon(),
                subject.getDescription(),
                subject.getStatus().name().toLowerCase(),
                subject.getProgress() != null ? subject.getProgress() : 0,
                derived.materialCount(),
                derived.noteCount(),
                derived.deckCount(),
                derived.studyMinutes(),
                derived.lastStudiedAt(),
                subject.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                subject.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
