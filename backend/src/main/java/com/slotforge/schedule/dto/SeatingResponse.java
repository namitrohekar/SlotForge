package com.slotforge.schedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** One hall of one paper, with the students seated there in roll-number order. */
public record SeatingResponse(
        LocalDate date, LocalTime start, LocalTime end, String paperCode, String paperName, String hallCode, String hallName,
        String building, List<Seat> students) {

    public record Seat(String rollNo, String fullName) {
    }
}
