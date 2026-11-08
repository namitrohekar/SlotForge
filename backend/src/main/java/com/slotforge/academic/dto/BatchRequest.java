package com.slotforge.academic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BatchRequest(
        @NotNull Long curriculumId,
        @NotBlank @Size(max = 50) String name,
        @NotNull @Min(2000) @Max(2100) Integer admissionYear,
        @NotNull @Min(1) Integer currentSemester) {
}
