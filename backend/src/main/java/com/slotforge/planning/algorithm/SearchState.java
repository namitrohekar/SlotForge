package com.slotforge.planning.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.slotforge.planning.allocation.HallPacker;
import com.slotforge.planning.allocation.HallPacker.Demand;
import com.slotforge.planning.graph.ConflictGraph;
import com.slotforge.planning.graph.ConflictGraphBuilder;
import com.slotforge.planning.model.Constraint;
import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallInfo;
import com.slotforge.planning.model.HallSeats;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.SlotInfo;

/**
 * A partial timetable. Exams and slots are referred to by position in their sorted lists.
 * Counters change on place and remove, so asking whether an exam fits a slot does no scanning.
 */
final class SearchState {

    // Spread a student's papers: indexed by days apart, capped at 3.
    private static final int[] SPACING_PENALTY = {10, 6, 2, 0};

    final List<ExamNode> exams;
    final List<SlotInfo> slots;
    final ConflictGraph graph;
    final int[] slotOf;
    final int[] saturation;
    int placed;

    private final List<List<HallInfo>> hallsBySlot;
    private final int maxPerDay;
    private final long[] epochOfDay;
    private final int[] dayOfSlot;
    private final int[][] nearDays;
    private final int[][] studentsOf;
    private final int[][] examsOf;
    private final boolean[] heavy;
    private final Constraint[][] fixedReason;
    private final int[][] clashes;
    private final int[][] studentDay;
    private final int[][] atDailyLimit;
    private final int[][] heavyBlocked;
    private final List<List<Integer>> inSlot = new ArrayList<>();
    private final int[] slotVersion;
    private final int[][] fitCheckedAt;
    private final boolean[][] fits;

    SearchState(SchedulingProblem problem) {
        exams = problem.exams().stream().sorted(Comparator.comparingLong(ExamNode::id)).toList();
        slots = problem.slots().stream()
                .sorted(Comparator.comparing(SlotInfo::date).thenComparing(SlotInfo::start).thenComparingLong(SlotInfo::id))
                .toList();
        graph = ConflictGraphBuilder.build(exams);
        maxPerDay = problem.rules().maxExamsPerStudentPerDay();
        hallsBySlot = slots.stream()
                .map(slot -> problem.halls().stream().filter(hall -> !hall.unavailableSlots().contains(slot.id())).toList())
                .toList();

        int examCount = exams.size();
        int slotCount = slots.size();
        slots.forEach(slot -> inSlot.add(new ArrayList<>()));
        slotOf = new int[examCount];
        Arrays.fill(slotOf, -1);
        saturation = new int[examCount];
        clashes = new int[examCount][slotCount];
        slotVersion = new int[slotCount];
        fits = new boolean[examCount][slotCount];
        fitCheckedAt = new int[examCount][slotCount];
        for (int[] row : fitCheckedAt) Arrays.fill(row, -1);

        long[] days = slots.stream().mapToLong(slot -> slot.date().toEpochDay()).distinct().sorted().toArray();
        epochOfDay = days;
        dayOfSlot = slots.stream().mapToInt(slot -> Arrays.binarySearch(days, slot.date().toEpochDay())).toArray();
        int gap = problem.rules().heavyGapDays();
        nearDays = new int[days.length][];
        for (int day = 0; day < days.length; day++) {
            long today = days[day];
            nearDays[day] = IntStream.range(0, days.length).filter(other -> Math.abs(days[other] - today) <= gap).toArray();
        }

        Map<Long, Integer> studentIndex = new HashMap<>();
        List<List<Integer>> examsByStudent = new ArrayList<>();
        studentsOf = new int[examCount][];
        heavy = new boolean[examCount];
        for (int exam = 0; exam < examCount; exam++) {
            heavy[exam] = exams.get(exam).heavy();
            int[] indices = new int[exams.get(exam).enrolled()];
            int next = 0;
            for (Long student : exams.get(exam).students()) {
                Integer index = studentIndex.get(student);
                if (index == null) {
                    index = studentIndex.size();
                    studentIndex.put(student, index);
                    examsByStudent.add(new ArrayList<>());
                }
                examsByStudent.get(index).add(exam);
                indices[next++] = index;
            }
            studentsOf[exam] = indices;
        }
        examsOf = examsByStudent.stream()
                .map(list -> list.stream().mapToInt(Integer::intValue).toArray())
                .toArray(int[][]::new);
        studentDay = new int[examsOf.length][days.length];
        atDailyLimit = new int[examCount][days.length];
        heavyBlocked = new int[examCount][days.length];

        fixedReason = new Constraint[examCount][slotCount];
        for (int exam = 0; exam < examCount; exam++) {
            for (int slot = 0; slot < slotCount; slot++) {
                fixedReason[exam][slot] = fixedReason(exams.get(exam), slots.get(slot));
            }
        }
    }

    private static Constraint fixedReason(ExamNode exam, SlotInfo slot) {
        if (!slot.open()) return Constraint.SLOT_CLOSED;
        if (slot.minutes() < exam.durationMinutes()) return Constraint.DURATION;
        if (exam.earliest() != null && slot.date().isBefore(exam.earliest())) return Constraint.WINDOW;
        if (exam.latest() != null && slot.date().isAfter(exam.latest())) return Constraint.WINDOW;
        return null;
    }

    int slotCount() {
        return slots.size();
    }

    void place(int exam, int slot) {
        slotOf[exam] = slot;
        placed++;
        inSlot.get(slot).add(exam);
        slotVersion[slot]++;
        for (int neighbour : graph.neighbours(exam)) {
            if (clashes[neighbour][slot]++ == 0) saturation[neighbour]++;
        }
        int day = dayOfSlot[slot];
        for (int student : studentsOf[exam]) {
            if (++studentDay[student][day] == maxPerDay) {
                for (int other : examsOf[student]) atDailyLimit[other][day]++;
            }
        }
        changeHeavyBlocks(exam, day, 1);
    }

    void remove(int exam) {
        int slot = slotOf[exam];
        slotOf[exam] = -1;
        placed--;
        inSlot.get(slot).remove(Integer.valueOf(exam));
        slotVersion[slot]++;
        for (int neighbour : graph.neighbours(exam)) {
            if (--clashes[neighbour][slot] == 0) saturation[neighbour]--;
        }
        int day = dayOfSlot[slot];
        for (int student : studentsOf[exam]) {
            if (studentDay[student][day]-- == maxPerDay) {
                for (int other : examsOf[student]) atDailyLimit[other][day]--;
            }
        }
        changeHeavyBlocks(exam, day, -1);
    }

    private void changeHeavyBlocks(int exam, int day, int change) {
        if (!heavy[exam]) return;
        for (int student : studentsOf[exam]) {
            for (int other : examsOf[student]) {
                if (other == exam || !heavy[other]) continue;
                for (int near : nearDays[day]) heavyBlocked[other][near] += change;
            }
        }
    }

    /** Why this slot cannot take this unplaced exam, or null if it can. */
    Constraint reason(int exam, int slot) {
        if (fixedReason[exam][slot] != null) return fixedReason[exam][slot];
        if (clashes[exam][slot] > 0) return Constraint.STUDENT_CLASH;
        int day = dayOfSlot[slot];
        if (atDailyLimit[exam][day] > 0) return Constraint.DAILY_LIMIT;
        if (heavyBlocked[exam][day] > 0) return Constraint.HEAVY_GAP;
        return hallsFit(exam, slot) ? null : Constraint.CAPACITY;
    }

    int legalCount(int exam) {
        int count = 0;
        for (int slot = 0; slot < slotCount(); slot++) {
            if (reason(exam, slot) == null) count++;
        }
        return count;
    }

    boolean hasLegalSlot(int exam) {
        for (int slot = 0; slot < slotCount(); slot++) {
            if (reason(exam, slot) == null) return true;
        }
        return false;
    }

    /** Legal slots, least crowding for the exam's students first, then earliest. */
    int[] orderedLegalSlots(int exam) {
        long[] keys = new long[slotCount()];
        int count = 0;
        for (int slot = 0; slot < slotCount(); slot++) {
            if (reason(exam, slot) == null) keys[count++] = spacingPenalty(exam, slot) * slotCount() + slot;
        }
        Arrays.sort(keys, 0, count);
        int[] ordered = new int[count];
        for (int index = 0; index < count; index++) ordered[index] = (int) (keys[index] % slotCount());
        return ordered;
    }

    /** Moves a placed exam to its least crowded legal slot, only if that is strictly better. */
    boolean moveToLessCrowdedSlot(int exam) {
        int current = slotOf[exam];
        remove(exam);
        int best = orderedLegalSlots(exam)[0];
        int target = spacingPenalty(exam, best) < spacingPenalty(exam, current) ? best : current;
        // Packing is greedy, so a slot can stop packing when a paper leaves it.
        if (target != current && HallPacker.pack(demands(current), hallsBySlot.get(current)).isEmpty()) target = current;
        place(exam, target);
        return target != current;
    }

    private long spacingPenalty(int exam, int slot) {
        long penalty = 0;
        for (int neighbour : graph.neighbours(exam)) {
            if (slotOf[neighbour] < 0) continue;
            long apart = Math.abs(epochOfDay[dayOfSlot[slot]] - epochOfDay[dayOfSlot[slotOf[neighbour]]]);
            penalty += (long) graph.weight(exam, neighbour) * SPACING_PENALTY[(int) Math.min(apart, 3)];
        }
        return penalty;
    }

    // Packing depends only on what the slot holds, so a result stays valid until that slot changes.
    private boolean hallsFit(int exam, int slot) {
        if (fitCheckedAt[exam][slot] != slotVersion[slot]) {
            fitCheckedAt[exam][slot] = slotVersion[slot];
            List<Demand> demands = demands(slot);
            demands.add(new Demand(exams.get(exam).id(), exams.get(exam).enrolled()));
            fits[exam][slot] = HallPacker.pack(demands, hallsBySlot.get(slot)).isPresent();
        }
        return fits[exam][slot];
    }

    private List<Demand> demands(int slot) {
        List<Demand> demands = new ArrayList<>();
        for (int exam : inSlot.get(slot)) demands.add(new Demand(exams.get(exam).id(), exams.get(exam).enrolled()));
        return demands;
    }

    /** Every slot was checked as it filled, so a slot that was accepted always packs. */
    Map<Long, List<HallSeats>> hallPlan(int slot) {
        return HallPacker.pack(demands(slot), hallsBySlot.get(slot)).orElseThrow();
    }

    String clashExample(int exam, int slot) {
        for (int neighbour : graph.neighbours(exam)) {
            if (slotOf[neighbour] == slot) {
                return exams.get(neighbour).code() + ", " + graph.weight(exam, neighbour) + " shared students";
            }
        }
        return null;
    }

    String capacityExample(int exam, int slot) {
        int free = hallsBySlot.get(slot).stream().mapToInt(HallInfo::capacity).sum();
        for (int other : inSlot.get(slot)) free -= exams.get(other).enrolled();
        return "needs " + exams.get(exam).enrolled() + " seats, " + Math.max(free, 0) + " free";
    }
}
