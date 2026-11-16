package com.slotforge.schedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

/** The public notice for one session: what is sat when and where, with nothing about who. */
public record PublishedTimetableResponse(
        long sessionId, String sessionName, LocalDate startDate, LocalDate endDate, OffsetDateTime publishedAt, List<Paper> papers) {

    /** halls holds hall codes. A paper split across halls lists each one. */
    public record Paper(LocalDate date, LocalTime start, LocalTime end, String paperCode, String paperName, List<String> halls) {
    }
}
