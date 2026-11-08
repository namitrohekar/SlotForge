package com.slotforge.auth.dto;

import jakarta.validation.constraints.NotNull;

public record DemoLoginRequest(@NotNull Long userId) {
}
