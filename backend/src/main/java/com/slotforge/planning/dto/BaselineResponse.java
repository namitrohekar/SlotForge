package com.slotforge.planning.dto;

import com.slotforge.planning.comparison.AlgorithmComparison.Measures;

/** Both algorithms run on one session's current data, measured the same way. Nothing is stored. */
public record BaselineResponse(int papers, int students, int openSlots, Measures dsatur, Measures firstFit) {
}
