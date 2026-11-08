package com.slotforge.planning.dto;

import java.util.List;

import com.slotforge.planning.model.Unplaced;

/** versionId and versionNo are null when nothing could be placed, because no draft is stored then. */
public record RunResponse(
        long runId, String status, Long versionId, Integer versionNo, int examsTotal, int examsScheduled, int slotsUsed,
        long searchNodes, long backtracks, long durationMs, List<Unplaced> unplaced) {
}
