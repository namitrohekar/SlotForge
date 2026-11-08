package com.slotforge.exam.dto;

import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FromCurriculumRequest(
        @NotEmpty @Size(max = 50) Set<@NotNull Long> batchIds,
        @NotNull @Min(15) @Max(480) Integer durationMinutes) {
}
