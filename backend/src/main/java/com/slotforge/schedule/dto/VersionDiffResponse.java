package com.slotforge.schedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** What differs between a timetable and the one it is compared with. againstVersionNo is null when there is nothing to compare with. */
public record VersionDiffResponse(int versionNo, Integer againstVersionNo, String againstStatus, Summary summary, List<Change> changes) {

    /** studentsAffected counts sittings: a student with two changed papers counts twice. */
    public record Summary(int papersMoved, int hallChanges, int invigilatorChanges, int added, int removed, int unchanged, int studentsAffected, int staffAffected) {
    }

    /** kind is MOVED, HALLS, INVIGILATORS, ADDED or REMOVED. before or after is null for a paper that was added or removed. */
    public record Change(String paperCode, String paperName, int students, String kind, Place before, Place after) {
    }

    public record Place(LocalDate date, LocalTime start, LocalTime end, List<String> halls, List<String> invigilators) {
    }
}
