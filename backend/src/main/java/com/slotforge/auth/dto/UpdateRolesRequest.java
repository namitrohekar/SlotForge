package com.slotforge.auth.dto;

import java.util.Set;

import com.slotforge.auth.entity.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UpdateRolesRequest(@NotEmpty Set<@NotNull Role> roles) {
}
