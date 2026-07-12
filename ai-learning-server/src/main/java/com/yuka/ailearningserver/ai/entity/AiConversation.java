package com.yuka.ailearningserver.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuka.ailearningserver.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * One AI Tutor conversation thread. {@code subjectName} is a snapshot, not a
 * logical FK — Subjects are still frontend-mock-only (see {@code
 * docs/ai-engine.md}), so there is no real {@code subjects} row to point at.
 */
@Getter
@Setter
@TableName("ai_conversations")
public class AiConversation extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Logical FK → users.id (owner). */
    private Long userId;

    private String title;

    /** Snapshot of the client-supplied mock subject name, if the chat started from a subject page. */
    private String subjectName;

    private Boolean archived;
}
