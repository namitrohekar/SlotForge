package com.slotforge.academic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotNull Long departmentId,
        @NotBlank @Size(max = 20) @Pattern(regexp = "\\s*([A-Za-z0-9][A-Za-z0-9-]*\\s*)?",
                message = "use letters, digits and hyphens only") String code,
        @NotBlank @Size(max = 150) String name,
        @NotNull @Min(1) @Max(12) Integer totalSemesters) {
}
