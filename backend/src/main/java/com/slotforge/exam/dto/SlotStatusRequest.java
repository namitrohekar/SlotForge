package com.slotforge.exam.dto;

import com.slotforge.exam.entity.SlotStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SlotStatusRequest(@NotNull SlotStatus status, @Size(max = 255) String reason) {
}
