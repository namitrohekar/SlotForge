package com.slotforge.planning.model;

import java.util.List;
import java.util.Map;

/** trace lists placed papers in the order the engine chose them. */
public record PlanResult(
        Outcome outcome, Map<Long, Long> slotByExam, Map<Long, List<HallSeats>> hallsByExam, List<Unplaced> unplaced,
        long searchNodes, long backtracks, List<PlacementStep> trace) {

    public PlanResult(
            Outcome outcome, Map<Long, Long> slotByExam, Map<Long, List<HallSeats>> hallsByExam, List<Unplaced> unplaced,
            long searchNodes, long backtracks) {
        this(outcome, slotByExam, hallsByExam, unplaced, searchNodes, backtracks, List.of());
    }
}
