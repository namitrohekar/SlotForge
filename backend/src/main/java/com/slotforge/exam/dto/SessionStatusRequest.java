package com.slotforge.exam.dto;

import com.slotforge.exam.entity.SessionStatus;
import jakarta.validation.constraints.NotNull;

public record SessionStatusRequest(@NotNull SessionStatus status) {
}
