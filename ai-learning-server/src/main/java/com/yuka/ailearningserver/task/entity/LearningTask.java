package com.yuka.ailearningserver.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuka.ailearningserver.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * A learning to-do, e.g. "finish chapter 4 exercises".
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
    private Long subjectId;

    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    /** When the task should be done; null = unscheduled (backlog). */
    private LocalDateTime dueAt;

    private LocalDateTime completedAt;
}
