package com.slotforge.planning.model;

import java.util.Set;

/** A hall, and the slots it cannot be used in because of outages or other bookings. */
public record HallInfo(long id, String code, int capacity, Set<Long> unavailableSlots) {
}
