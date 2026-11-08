package com.slotforge.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReturnRequest(@NotBlank @Size(max = 500) String comment) {
}
