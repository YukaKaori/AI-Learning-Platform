package com.yuka.ailearningserver.subject.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * Lifecycle of a subject, stored as {@code subjects.status}.
 */
public enum SubjectStatus {

    /** Actively being studied — shows up in the workspace. */
    ACTIVE(0),
    /** Finished — kept for review and analytics. */
    COMPLETED(1),
    /** Shelved — hidden from the workspace, restorable at any time. */
    ARCHIVED(2);

    @EnumValue
    private final int code;

    SubjectStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
