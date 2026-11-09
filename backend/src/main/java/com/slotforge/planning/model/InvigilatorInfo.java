package com.slotforge.planning.model;

import java.util.Set;

/** Someone who can invigilate, and the slots they cannot take because of leave or a duty in another session. */
public record InvigilatorInfo(long id, Set<Long> unavailableSlots) {
}
