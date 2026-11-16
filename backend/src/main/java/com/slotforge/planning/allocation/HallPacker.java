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

    public static Optional<Map<Long, List<HallSeats>>> pack(List<Demand> demands, List<HallInfo> halls) {
        return pack(demands, halls, Map.of());
    }

    /**
     * keep names halls a paper already had, in seating order. Each one still free keeps its seats, so those students stay put.
     * Seats in a hall that is gone are packed afresh into the same place in the order, so only those students move.
     * If that leaves any paper unable to fit, everything is packed afresh: a hall change beats a paper left out.
     */
    public static Optional<Map<Long, List<HallSeats>>> pack(List<Demand> demands, List<HallInfo> halls, Map<Long, List<HallSeats>> keep) {
        List<HallInfo> free = sortedFree(halls);
        Map<Long, List<HallSeats>> plan = new LinkedHashMap<>();
        Map<Long, List<Boolean>> lost = new LinkedHashMap<>();
        List<Demand> open = new ArrayList<>();
        for (Demand demand : demands.stream().sorted(Comparator.comparingLong(Demand::examId)).toList()) {
            List<HallSeats> before = keep.get(demand.examId());
            if (before == null || before.isEmpty() || before.stream().mapToInt(HallSeats::seats).sum() != demand.seats()) {
                open.add(demand);
                continue;
            }
            List<Boolean> gone = new ArrayList<>();
            for (HallSeats hall : before) {
                boolean usable = free.removeIf(candidate -> candidate.id() == hall.hallId() && candidate.capacity() >= hall.seats());
                gone.add(!usable);
            }
            plan.put(demand.examId(), before);
            if (gone.contains(true)) lost.put(demand.examId(), gone);
        }
        // Largest displaced group first, as when packing whole papers.
        List<Long> displaced = new ArrayList<>(lost.keySet());
        displaced.sort(Comparator.comparingInt((Long exam) -> -missing(plan.get(exam), lost.get(exam))).thenComparingLong(exam -> exam));
        for (Long exam : displaced) {
            List<HallSeats> refilled = new ArrayList<>();
            List<HallSeats> before = plan.get(exam);
            for (int index = 0; index < before.size(); index++) {
                if (!lost.get(exam).get(index)) {
                    refilled.add(before.get(index));
                    continue;
                }
                Optional<Map<Long, List<HallSeats>>> replacement = packGreedily(List.of(new Demand(exam, before.get(index).seats())), free);
                if (replacement.isEmpty()) return packGreedily(demands, sortedFree(halls));
                refilled.addAll(replacement.get().get(exam));
            }
            plan.put(exam, refilled);
        }
        Optional<Map<Long, List<HallSeats>>> rest = packGreedily(open, free);
        if (rest.isEmpty()) return plan.isEmpty() ? rest : packGreedily(demands, sortedFree(halls));
        plan.putAll(rest.get());
        return Optional.of(plan);
    }

    private static int missing(List<HallSeats> before, List<Boolean> gone) {
        int seats = 0;
        for (int index = 0; index < before.size(); index++) {
            if (gone.get(index)) seats += before.get(index).seats();
        }
        return seats;
    }

    private static List<HallInfo> sortedFree(List<HallInfo> halls) {
        List<HallInfo> free = new ArrayList<>(halls);
        free.sort(Comparator.comparingInt(HallInfo::capacity).thenComparingLong(HallInfo::id));
        return free;
    }

    // shortcut: greedy, so it can reject a combination that does fit (see HallPackerTest), upgrade to an exact search for small slots if reports show it.
    /** free must be sorted by capacity then id, and is consumed. */
    private static Optional<Map<Long, List<HallSeats>>> packGreedily(List<Demand> demands, List<HallInfo> free) {
        List<Demand> largestFirst = new ArrayList<>(demands);
        largestFirst.sort(Comparator.comparingInt(Demand::seats).reversed().thenComparingLong(Demand::examId));

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
