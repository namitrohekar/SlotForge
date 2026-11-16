package com.slotforge.schedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ScheduleEntryResponse(
        long examinationId, String subjectCode, String subjectName, boolean heavy, String departmentCode, int durationMinutes,
        long slotId, LocalDate date, LocalTime start, LocalTime end, int students, List<Hall> halls, List<Conflict> conflicts,
        boolean locked, String source) {

    public record Hall(String code, String name, String building, int seats, int capacity, List<String> invigilators) {
    }

    /** Another paper in this timetable that shares students with this one. */
    public record Conflict(long examinationId, int sharedStudents) {
    }
}
