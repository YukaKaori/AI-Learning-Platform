package com.yuka.ailearningserver.task.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuka.ailearningserver.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * A learning to-do, e.g. "finish chapter 4 exercises".
 *
 * <p>Nullable columns that services must be able to clear (unlink subject,
 * unschedule, undo completion) use update strategy ALWAYS — safe because
 * services always load the row before updating it.
 */
@Getter
@Setter
@TableName("learning_tasks")
public class LearningTask extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Logical FK → users.id. */
    private Long userId;

    /** Logical FK → subjects.id; null for subject-independent tasks. */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long subjectId;

    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    /** When the task should be done; null = unscheduled (backlog). */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime dueAt;

    /** Owned by the status transition — set on entering done, cleared on leaving it. */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime completedAt;
}
