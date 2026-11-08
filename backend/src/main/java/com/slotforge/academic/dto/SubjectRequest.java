package com.slotforge.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
        @NotNull Long departmentId,
        @NotBlank @Size(max = 20) @Pattern(regexp = "\\s*([A-Za-z0-9][A-Za-z0-9-]*\\s*)?",
                message = "use letters, digits and hyphens only") String code,
        @NotBlank @Size(max = 150) String name,
        Boolean heavy) {

    // Optional, and "not heavy" when left out. Jackson 3 rejects a missing primitive, so it is a Boolean.
    public SubjectRequest {
        if (heavy == null) heavy = false;
    }
}
