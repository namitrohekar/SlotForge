package com.slotforge.planning.allocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.slotforge.planning.model.HallInfo;
import com.slotforge.planning.model.HallSeats;

/** Fits the exams of one slot into halls, at most one exam per hall. An exam too big for one hall splits. */
public final class HallPacker {

    public record Demand(long examId, int seats) {
    }

    private HallPacker() {
    }

    // shortcut: greedy, so it can reject a combination that does fit (see HallPackerTest), upgrade to an exact search for small slots if reports show it.
    public static Optional<Map<Long, List<HallSeats>>> pack(List<Demand> demands, List<HallInfo> halls) {
        List<Demand> largestFirst = new ArrayList<>(demands);
        largestFirst.sort(Comparator.comparingInt(Demand::seats).reversed().thenComparingLong(Demand::examId));
        List<HallInfo> free = new ArrayList<>(halls);
        free.sort(Comparator.comparingInt(HallInfo::capacity).thenComparingLong(HallInfo::id));

        Map<Long, List<HallSeats>> plan = new LinkedHashMap<>();
        for (Demand demand : largestFirst) {
            List<HallSeats> used = new ArrayList<>();
            int remaining = demand.seats();
            while (remaining > 0) {
                int fitting = smallestHolding(free, remaining);
                if (fitting >= 0) {
                    used.add(new HallSeats(free.remove(fitting).id(), remaining));
                    remaining = 0;
                } else if (free.isEmpty()) {
                    return Optional.empty();
                } else {
                    HallInfo largest = free.remove(free.size() - 1);
                    used.add(new HallSeats(largest.id(), largest.capacity()));
                    remaining -= largest.capacity();
                }
            }
            plan.put(demand.examId(), used);
        }
        return Optional.of(plan);
    }

    private static int smallestHolding(List<HallInfo> ascending, int seats) {
        for (int index = 0; index < ascending.size(); index++) {
            if (ascending.get(index).capacity() >= seats) return index;
        }
        return -1;
    }
}
