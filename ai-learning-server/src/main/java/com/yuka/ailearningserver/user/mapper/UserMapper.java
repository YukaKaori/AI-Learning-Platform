package com.yuka.ailearningserver.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuka.ailearningserver.user.entity.User;

import java.util.Optional;

public interface UserMapper extends BaseMapper<User> {

    /**
     * Looks a user up by login identifier — username or email, indistinguishable at login.
     */
    default Optional<User> findByUsernameOrEmail(String identifier) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getUsername, identifier).or().eq(User::getEmail, identifier))));
    }
}
