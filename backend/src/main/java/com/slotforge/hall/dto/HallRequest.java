package com.slotforge.hall.dto;

import com.slotforge.hall.entity.HallStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HallRequest(
        @NotBlank @Size(max = 20) @Pattern(regexp = "\\s*([A-Za-z0-9][A-Za-z0-9-]*\\s*)?",
                message = "use letters, digits and hyphens only") String code,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 80) String building,
        @Min(0) @Max(50) Integer floor,
        @NotNull @Min(1) @Max(2000) Integer capacity,
        HallStatus status) {

    public HallRequest {
        if (status == null) status = HallStatus.AVAILABLE;
    }
}
