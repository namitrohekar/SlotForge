package com.slotforge.exam.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.slotforge.exam.entity.SlotStatus;
import com.slotforge.exam.entity.TimeSlot;

public record TimeSlotResponse(
        Long id, LocalDate date, LocalTime start, LocalTime end, SlotStatus status, String blockReason, long usageCount) {

    public static TimeSlotResponse from(TimeSlot slot, long usageCount) {
        return new TimeSlotResponse(slot.getId(), slot.getSlotDate(), slot.getStartTime(), slot.getEndTime(), slot.getStatus(),
                slot.getBlockReason(), usageCount);
    }
}
