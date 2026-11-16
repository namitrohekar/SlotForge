package com.slotforge.planning.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.slotforge.planning.model.Constraint;

/** reason is null when the paper may move to this slot. current marks the slot it is in now. */
public record CandidateSlotResponse(
        long slotId, LocalDate date, LocalTime start, LocalTime end, boolean current, Constraint reason, String example) {
}
