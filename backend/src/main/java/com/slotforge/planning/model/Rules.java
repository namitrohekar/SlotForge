package com.slotforge.planning.model;

/** Two heavy papers for one student must be more than heavyGapDays apart. */
public record Rules(int maxExamsPerStudentPerDay, int heavyGapDays, long nodeBudget) {
}
