package com.voyage.user.dto;

import com.voyage.user.domain.User;

public record UserResponse(
        Long id,
        String email,
        String name,
        String avatarUrl,
        String defaultCurrency,
        String timezone
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAvatarUrl(),
                user.getDefaultCurrency(),
                user.getTimezone()
        );
    }
}
