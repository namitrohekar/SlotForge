package com.slotforge.planning.dto;

import jakarta.validation.constraints.NotNull;

public record LockRequest(@NotNull Boolean locked) {
}
