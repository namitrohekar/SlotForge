package com.slotforge.planning.model;

/**
 * One paper as the engine placed it: how cornered it was at that moment and where it went.
 * saturation is how many different slots its clashing papers had already taken. slotId is the first choice,
 * which the later spacing pass may still change.
 */
public record PlacementStep(long examId, int saturation, int legalSlots, int conflicts, long slotId) {
}
