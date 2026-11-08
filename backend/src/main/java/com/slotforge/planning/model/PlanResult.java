package com.slotforge.planning.model;

import java.util.List;
import java.util.Map;

public record PlanResult(
        Outcome outcome, Map<Long, Long> slotByExam, Map<Long, List<HallSeats>> hallsByExam, List<Unplaced> unplaced,
        long searchNodes, long backtracks) {
}
