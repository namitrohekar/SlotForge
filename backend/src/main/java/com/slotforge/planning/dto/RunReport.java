package com.slotforge.planning.dto;

import java.util.List;

import com.slotforge.planning.model.Unplaced;

/** What is stored in scheduling_run.report: everything the engine could not place, and why. */
public record RunReport(List<Unplaced> unplaced) {
}
