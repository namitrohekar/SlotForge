package com.slotforge.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StudentRequest(
        @NotNull Long batchId,
        @NotBlank @Size(max = 30) @Pattern(regexp = "\\s*([A-Za-z0-9][A-Za-z0-9/-]*\\s*)?",
                message = "use letters, digits, hyphens and slashes only") String rollNo,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 254) String email) {
}
