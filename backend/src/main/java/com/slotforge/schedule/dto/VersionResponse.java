package com.slotforge.schedule.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.slotforge.planning.model.PlacementStep;
import com.slotforge.planning.model.Unplaced;
import com.slotforge.planning.quality.QualityScorer.Quality;

public record VersionResponse(
        long id, int versionNo, String status, OffsetDateTime createdAt, Integer examsTotal, Integer examsScheduled,
        Integer slotsUsed, Long searchNodes, Long backtracks, Long durationMs, List<Unplaced> unplaced, String reviewComment,
        String reviewedBy, OffsetDateTime reviewedAt, OffsetDateTime publishedAt, OffsetDateTime submittedAt,
        String generatedBy, String sentBy, String publishedBy, int invigilatorCount, List<PlacementStep> trace, Quality quality) {
}
