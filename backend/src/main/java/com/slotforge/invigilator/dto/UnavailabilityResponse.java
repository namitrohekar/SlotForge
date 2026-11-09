package com.slotforge.invigilator.dto;

import java.time.LocalDate;

/** dutiesAffected counts published duties already on these dates. */
public record UnavailabilityResponse(long id, LocalDate fromDate, LocalDate toDate, String reason, int dutiesAffected) {
}
