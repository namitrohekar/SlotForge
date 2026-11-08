package com.slotforge.exam.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** weekdays are ISO numbers, 1 for Monday to 5 for Friday. The college is closed at weekends. */
public record GenerateSlotsRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate to,
        @NotEmpty Set<@NotNull @Min(1) @Max(5) Integer> weekdays,
        @NotEmpty @Size(max = 6) List<@Valid @NotNull TimeWindow> times) {
}
