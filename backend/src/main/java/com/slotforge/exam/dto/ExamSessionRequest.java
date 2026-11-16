package com.slotforge.exam.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExamSessionRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @Min(1) @Max(4) Integer maxExamsPerStudentPerDay,
        @NotNull @Min(0) @Max(14) Integer heavyGapDays,
        // Optional, so a client that does not know the staffing rules leaves them as they are.
        @Min(1) @Max(200) Integer studentsPerInvigilator,
        @Min(1) @Max(40) Integer maxDutiesPerInvigilator) {
}
