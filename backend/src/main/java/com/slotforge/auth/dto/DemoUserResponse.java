package com.slotforge.auth.dto;

import java.util.List;

import com.slotforge.auth.entity.AppUser;

public record DemoUserResponse(Long id, String fullName, String email, List<String> roles) {

    public static DemoUserResponse from(AppUser user) {
        return new DemoUserResponse(user.getId(), user.getFullName(), user.getEmail(),
                user.getRoles().stream().map(Enum::name).sorted().toList());
    }
}
