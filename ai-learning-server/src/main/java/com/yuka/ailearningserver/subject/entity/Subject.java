package com.yuka.ailearningserver.subject.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuka.ailearningserver.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * A subject the user is learning (e.g. "Machine Learning", "Japanese").
 * Owns materials, notes, decks, tasks and study sessions via logical FKs.
 *
 * <p>{@code studyMinutes} and material counts are <em>derived</em> from
 * {@code study_sessions} / {@code learning_materials} — only {@code progress}
 * is denormalized (user-visible, user-adjustable).
 */
@Getter
@Setter
@TableName("subjects")
public class Subject extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Logical FK → users.id. Subjects are strictly per-user in Phase 5. */
    private Long userId;

    private String name;

    /** Accent color as a hex string (e.g. {@code #5e6ad2}); rendered by the frontend. */
    private String color;

    /** Icon name from the frontend icon registry (e.g. {@code brain}). */
    private String icon;

    private String description;

    private SubjectStatus status;

    /** Completion percentage 0–100, curated by the user until analytics derives it. */
    private Integer progress;
}
