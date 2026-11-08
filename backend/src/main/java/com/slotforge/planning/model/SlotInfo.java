package com.slotforge.planning.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/** One dated exam period: a colour of the conflict graph. */
public record SlotInfo(long id, LocalDate date, LocalTime start, LocalTime end, boolean open) {

    public long minutes() {
        return Duration.between(start, end).toMinutes();
    }
}
