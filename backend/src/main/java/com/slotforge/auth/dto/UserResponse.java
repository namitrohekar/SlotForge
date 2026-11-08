package com.slotforge.auth.dto;

import java.time.Instant;
import java.util.List;

import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.UserStatus;

public record UserResponse(
        Long id, String email, String fullName, List<String> roles, UserStatus status, Instant lastLoginAt,
        Instant createdAt) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getRoles().stream().map(Enum::name).sorted().toList(), user.getStatus(), user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
