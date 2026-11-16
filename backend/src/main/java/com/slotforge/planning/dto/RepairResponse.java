package com.slotforge.planning.dto;

/** level 0: nothing moved. 1: only papers that had to. 2: also the papers clashing with them. 3: rebuilt from scratch. */
public record RepairResponse(RunResponse run, int level, int papersMoved) {
}
