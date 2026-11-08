package com.slotforge.exam.dto;

import java.util.List;

/** latestVersionNo is 0, and the other latest fields null, until a timetable has been generated. */
public record SessionProgress(
        int openSlots, int slotsTooShort, Integer shortestPaperMinutes, int readyPapers, List<String> papersWithoutStudents,
        int latestVersionNo, String latestVersionStatus, Integer latestPlaced, Integer latestTotal, int activeApprovers) {
}
