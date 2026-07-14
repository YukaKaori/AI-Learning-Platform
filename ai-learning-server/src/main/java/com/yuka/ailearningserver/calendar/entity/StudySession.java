package com.yuka.ailearningserver.calendar.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuka.ailearningserver.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * A block of study time — planned ahead of time or logged after the fact.
 * Duration is derived ({@code endsAt - startsAt}), never stored.
 *
 * <p>Nullable columns that services must be able to clear (unlink subject,
 * remove label) use update strategy ALWAYS — safe because services always
 * load the row before updating it.
 */
@Getter
@Setter
@TableName("study_sessions")
public class StudySession extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Logical FK → users.id. */
    private Long userId;

    /** Logical FK → subjects.id; null for unclassified sessions. */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long subjectId;

    /** Optional label, e.g. "Deep work: chapter 4". */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String title;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;
}
