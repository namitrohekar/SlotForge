package com.slotforge.exam.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record TimeWindow(@NotNull LocalTime start, @NotNull LocalTime end) {
}
