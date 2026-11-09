package com.slotforge.exam.dto;

import java.util.List;
import java.util.Set;

/** latestVersionNo is 0, and the other latest fields null, until a timetable has been generated. inReviewStatus is null when nothing is with the approver. */
public record SessionProgress(
        int openSlots, int slotsTooShort, Integer shortestPaperMinutes, int readyPapers, List<String> papersWithoutStudents,
        int latestVersionNo, String latestVersionStatus, Integer latestPlaced, Integer latestTotal, String inReviewStatus, int latestProblems,
        String approverPost, String approverName, Set<Long> approverIds) {
}
