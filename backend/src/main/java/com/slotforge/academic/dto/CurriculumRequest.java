package com.slotforge.academic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CurriculumRequest(
        @NotNull Long courseId,
        @NotBlank @Size(max = 80) String name,
        @NotNull @Min(2000) @Max(2100) Integer effectiveFromYear) {
}
