package com.slotforge.planning.dto;

import jakarta.validation.constraints.NotNull;

public record MoveRequest(@NotNull Long slotId) {
}
