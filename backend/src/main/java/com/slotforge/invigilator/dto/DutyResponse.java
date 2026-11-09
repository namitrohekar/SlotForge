package com.slotforge.invigilator.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record DutyResponse(
        LocalDate date, LocalTime start, LocalTime end, String hallCode, String hallName, String building,
        String paperCode, String paperName, int students, String sessionName, boolean unavailable) {
}
