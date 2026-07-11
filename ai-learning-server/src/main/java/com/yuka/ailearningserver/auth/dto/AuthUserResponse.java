package com.yuka.ailearningserver.auth.dto;

import com.yuka.ailearningserver.user.entity.User;

/**
 * Public view of the authenticated account. The snowflake id is serialized as a
 * string — it exceeds JavaScript's safe-integer range.
 */
public record AuthUserResponse(String id, String username, String email, String nickname, String avatar) {

    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatar());
    }
}
