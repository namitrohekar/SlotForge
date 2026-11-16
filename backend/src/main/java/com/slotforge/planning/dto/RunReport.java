package com.slotforge.planning.dto;

import java.util.List;

import com.slotforge.planning.model.PlacementStep;
import com.slotforge.planning.model.Unplaced;
import com.slotforge.planning.quality.QualityScorer.Quality;

/**
 * What is stored in scheduling_run.report: what could not be placed and why, the order papers were placed in,
 * and how good the result is. trace and quality are null for runs saved before they were recorded.
 */
public record RunReport(List<Unplaced> unplaced, List<PlacementStep> trace, Quality quality) {
}
