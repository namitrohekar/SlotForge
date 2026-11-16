package com.slotforge.hall.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HallOutageRequest(@NotNull LocalDate fromDate, @NotNull LocalDate toDate, @NotBlank @Size(max = 255) String reason) {
}
