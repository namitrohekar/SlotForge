package com.slotforge.planning.model;

import java.util.List;
import java.util.Map;

/**
 * What an earlier timetable hands to a new run.
 * fixedSlot: papers that stay where they are while that is still legal. A pin that has become illegal is released, not forced.
 * keepHalls: the halls a paper had, reused while they still fit, so students are not sent to a new room for nothing.
 * homeSlot: where a released paper used to be. It is placed as near that date as possible instead of wherever spacing is best.
 */
public record Pins(Map<Long, Long> fixedSlot, Map<Long, List<HallSeats>> keepHalls, Map<Long, Long> homeSlot) {

    public static Pins none() {
        return new Pins(Map.of(), Map.of(), Map.of());
    }
}
