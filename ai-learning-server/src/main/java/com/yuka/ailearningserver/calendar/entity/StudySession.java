package com.yuka.ailearningserver.calendar.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuka.ailearningserver.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * A block of study time — planned ahead of time or logged after the fact.
 * Duration is derived ({@code endsAt - startsAt}), never stored.
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
    private Long subjectId;

    private String title;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;
}
