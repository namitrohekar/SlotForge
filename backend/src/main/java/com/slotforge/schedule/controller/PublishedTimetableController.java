package com.slotforge.schedule.controller;

import java.util.List;

import com.slotforge.schedule.dto.PublishedTimetableResponse;
import com.slotforge.schedule.service.ScheduleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** A published timetable is a notice, so everyone who can sign in may read it. */
@RestController
@RequiredArgsConstructor
public class PublishedTimetableController {

    private final ScheduleQueryService schedules;

    @GetMapping("/api/published-timetables")
    @PreAuthorize("isAuthenticated()")
    public List<PublishedTimetableResponse> published() {
        return schedules.published();
    }
}
