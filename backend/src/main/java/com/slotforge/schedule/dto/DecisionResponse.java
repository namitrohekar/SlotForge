package com.slotforge.schedule.dto;

import java.time.OffsetDateTime;

/** A timetable this person approved, and what has become of it since. */
public record DecisionResponse(long sessionId, String sessionName, int versionNo, String status, OffsetDateTime reviewedAt) {
}
