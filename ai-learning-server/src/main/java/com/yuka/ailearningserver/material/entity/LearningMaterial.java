package com.yuka.ailearningserver.material.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuka.ailearningserver.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * A single piece of learning content owned by a subject. Exactly one of
 * {@code sourceUrl} (external content) or {@code storageKey} (uploaded
 * content, resolved via the future StorageService) is expected to be set —
 * both stay null until upload/link flows are implemented.
 */
@Getter
@Setter
@TableName("learning_materials")
public class LearningMaterial extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Logical FK → subjects.id. */
    private Long subjectId;

    /** Logical FK → users.id — denormalized for per-user queries without a join. */
    private Long userId;

    private String title;

    private MaterialType type;

    private String description;

    /** External location for LINK / ARTICLE / VIDEO materials. */
    private String sourceUrl;

    /** Opaque key in the storage backend for uploaded files (StorageService, future). */
    private String storageKey;

    /** Size in bytes for uploaded files; null for external content. */
    private Long sizeBytes;
}
