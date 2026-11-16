package com.slotforge.hall.dto;

import java.time.LocalDate;

/** papersAffected counts papers of live timetables placed in this hall on these dates. */
public record HallOutageResponse(long id, LocalDate fromDate, LocalDate toDate, String reason, int papersAffected) {
}
