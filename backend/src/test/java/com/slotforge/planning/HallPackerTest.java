package com.slotforge.planning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import com.slotforge.planning.allocation.HallPacker;
import com.slotforge.planning.allocation.HallPacker.Demand;
import com.slotforge.planning.model.HallInfo;
import com.slotforge.planning.model.HallSeats;
import org.junit.jupiter.api.Test;

class HallPackerTest {

    private static HallInfo hall(long id, int capacity) {
        return new HallInfo(id, "H" + id, capacity, Set.of());
    }

    @Test
    void anExamTakesTheSmallestHallThatHoldsIt() {
        var plan = HallPacker.pack(List.of(new Demand(1, 40)), List.of(hall(1, 100), hall(2, 50), hall(3, 30))).orElseThrow();

        assertThat(plan.get(1L)).containsExactly(new HallSeats(2, 40));
    }

    @Test
    void anExamTooBigForOneHallSplits() {
        var plan = HallPacker.pack(List.of(new Demand(1, 150)), List.of(hall(1, 100), hall(2, 100))).orElseThrow();

        assertThat(plan.get(1L)).extracting(HallSeats::seats).containsExactly(100, 50);
    }

    @Test
    void aHallHoldsOneExamEvenWithSeatsToSpare() {
        assertThat(HallPacker.pack(List.of(new Demand(1, 10), new Demand(2, 10)), List.of(hall(1, 500)))).isEmpty();
    }

    @Test
    void anExamWithNoStudentsNeedsNoHall() {
        var plan = HallPacker.pack(List.of(new Demand(1, 0)), List.of()).orElseThrow();

        assertThat(plan.get(1L)).isEmpty();
    }

    @Test
    void tooFewSeatsDoNotPack() {
        assertThat(HallPacker.pack(List.of(new Demand(1, 201)), List.of(hall(1, 100), hall(2, 100)))).isEmpty();
    }

    // Documents the known greedy limit: 30+30+30 for the 90 and 70 for the 70 would work.
    @Test
    void theGreedyPackerCanMissAFitThatExists() {
        var halls = List.of(hall(1, 70), hall(2, 30), hall(3, 30), hall(4, 30));

        assertThat(HallPacker.pack(List.of(new Demand(1, 90), new Demand(2, 70)), halls)).isEmpty();
    }
}
