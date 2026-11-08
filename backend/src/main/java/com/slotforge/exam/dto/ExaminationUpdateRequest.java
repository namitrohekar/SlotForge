package com.slotforge.exam.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExaminationUpdateRequest(
        @NotNull @Min(15) @Max(480) Integer durationMinutes, LocalDate earliestDate, LocalDate latestDate) {
}
