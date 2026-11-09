package com.slotforge.schedule.dto;

import java.time.LocalDate;

public record DutyAtRiskResponse(long sessionId, String sessionName, String staffName, String paperCode, String hallCode, LocalDate date) {
}
