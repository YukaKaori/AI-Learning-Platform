package com.yuka.ailearningserver.note.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuka.ailearningserver.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * A markdown note. Content is stored as raw markdown; rendering, outline
 * extraction and excerpts are frontend/read-model concerns.
 */
@Getter
@Setter
@TableName("notes")
public class Note extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Logical FK → users.id. */
    private Long userId;

    /**
     * Logical FK → subjects.id; null for free-standing notes. Update strategy
     * ALWAYS so unlinking (setting null) persists via {@code updateById} —
     * services always load the row before updating it.
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long subjectId;

    private String title;

    /** Raw markdown source. */
    private String content;

    /** Pinned notes float to the top of the notes sidebar. */
    private Boolean pinned;
}
