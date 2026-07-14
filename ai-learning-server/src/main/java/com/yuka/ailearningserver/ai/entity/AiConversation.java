package com.yuka.ailearningserver.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuka.ailearningserver.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * One AI Tutor conversation thread. {@code subjectId} links the chat to a real
 * subject (V5); {@code subjectName} is kept as a denormalized display snapshot
 * so lists stay readable after the subject is renamed or deleted.
 *
 * <p>The subject link is clearable (the {@code subjectId = ""} sentinel on
 * send), so both columns use update strategy ALWAYS — safe because the
 * service always loads the row before updating it.
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

    /** Logical FK → subjects.id; null = no subject context. */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long subjectId;

    /** Denormalized subject name snapshot for display; survives subject rename/delete. */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String subjectName;

    private Boolean archived;
}
