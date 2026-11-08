package com.slotforge.planning.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.slotforge.planning.model.Constraint;
import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallSeats;
import com.slotforge.planning.model.Outcome;
import com.slotforge.planning.model.PlanResult;
import com.slotforge.planning.model.Rejection;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.Unplaced;

/**
 * DSATUR ordering inside a bounded backtracking search. Nothing here is random and the limit counts
 * search nodes rather than time, so the same input always gives the same timetable.
 */
public final class DsaturScheduler {

    private enum Step { COMPLETE, DEAD_END, BUDGET_EXHAUSTED }

    private final SchedulingProblem problem;
    private final long budget;
    private SearchState state;
    private final int[] deepest;
    private int deepestPlaced;
    private final boolean[] abandoned;
    private long nodes;
    private long backtracks;

    private DsaturScheduler(SchedulingProblem problem) {
        this.problem = problem;
        this.budget = problem.rules().nodeBudget();
        this.state = new SearchState(problem);
        this.deepest = new int[state.exams.size()];
        Arrays.fill(deepest, -1);
        this.abandoned = new boolean[state.exams.size()];
    }

    public static PlanResult schedule(SchedulingProblem problem) {
        return new DsaturScheduler(problem).run();
    }

    private PlanResult run() {
        Step end = search();
        if (end != Step.COMPLETE) placeWhatWeCan();
        else improveSpacing();

        List<Unplaced> unplaced = new ArrayList<>();
        for (int exam = 0; exam < state.exams.size(); exam++) {
            if (state.slotOf[exam] < 0) unplaced.add(unplaced(exam));
        }
        Outcome outcome = unplaced.isEmpty() ? Outcome.COMPLETE
                : end == Step.BUDGET_EXHAUSTED ? Outcome.BUDGET_EXHAUSTED : Outcome.NO_SOLUTION;
        return new PlanResult(outcome, slotsByExam(), hallsByExam(), unplaced, nodes, backtracks);
    }

    /** A paper placed early could not see the papers placed after it, so each one is looked at again. */
    private void improveSpacing() {
        boolean moved = true;
        while (moved && nodes < budget) {
            moved = false;
            for (int exam = 0; exam < state.exams.size() && nodes < budget; exam++) {
                nodes++;
                moved |= state.moveToLessCrowdedSlot(exam);
            }
        }
    }

    private Step search() {
        if (state.placed == state.exams.size()) return Step.COMPLETE;
        if (nodes >= budget) return Step.BUDGET_EXHAUSTED;

        int exam = nextExam();
        for (int slot : state.orderedLegalSlots(exam)) {
            state.place(exam, slot);
            nodes++;
            rememberIfDeepest();
            Step result = neighboursStillPlaceable(exam) ? search() : Step.DEAD_END;
            if (result != Step.DEAD_END) return result;
            state.remove(exam);
            backtracks++;
            if (nodes >= budget) return Step.BUDGET_EXHAUSTED;
        }
        return Step.DEAD_END;
    }

    /** Highest saturation, then fewest legal slots, most conflicts, most students, lowest id. */
    private int nextExam() {
        int highest = -1;
        for (int exam = 0; exam < state.exams.size(); exam++) {
            if (isOpen(exam)) highest = Math.max(highest, state.saturation[exam]);
        }
        int best = -1;
        int bestLegal = 0;
        for (int exam = 0; exam < state.exams.size(); exam++) {
            if (!isOpen(exam) || state.saturation[exam] != highest) continue;
            int legal = state.legalCount(exam);
            if (best < 0 || comesBefore(exam, legal, best, bestLegal)) {
                best = exam;
                bestLegal = legal;
            }
        }
        return best;
    }

    private boolean isOpen(int exam) {
        return state.slotOf[exam] < 0 && !abandoned[exam];
    }

    // Ties on every test keep the earlier exam, which is the lowest id because exams are sorted.
    private boolean comesBefore(int exam, int legal, int best, int bestLegal) {
        if (legal != bestLegal) return legal < bestLegal;
        if (state.graph.degree(exam) != state.graph.degree(best)) return state.graph.degree(exam) > state.graph.degree(best);
        return state.exams.get(exam).enrolled() > state.exams.get(best).enrolled();
    }

    private boolean neighboursStillPlaceable(int exam) {
        for (int neighbour : state.graph.neighbours(exam)) {
            if (state.slotOf[neighbour] < 0 && !state.hasLegalSlot(neighbour)) return false;
        }
        return true;
    }

    private void rememberIfDeepest() {
        if (state.placed > deepestPlaced) {
            deepestPlaced = state.placed;
            System.arraycopy(state.slotOf, 0, deepest, 0, deepest.length);
        }
    }

    /** A failed search still returns as much as it can: the deepest placement, extended greedily. */
    private void placeWhatWeCan() {
        state = new SearchState(problem);
        for (int exam = 0; exam < deepest.length; exam++) {
            if (deepest[exam] >= 0) state.place(exam, deepest[exam]);
        }
        for (int exam = nextExam(); exam >= 0; exam = nextExam()) {
            int[] slots = state.orderedLegalSlots(exam);
            if (slots.length == 0) abandoned[exam] = true;
            else state.place(exam, slots[0]);
        }
    }

    private Unplaced unplaced(int exam) {
        Map<Constraint, Integer> counts = new EnumMap<>(Constraint.class);
        Map<Constraint, String> examples = new EnumMap<>(Constraint.class);
        for (int slot = 0; slot < state.slotCount(); slot++) {
            Constraint reason = state.reason(exam, slot);
            if (reason == null) continue;
            counts.merge(reason, 1, Integer::sum);
            if (reason == Constraint.STUDENT_CLASH && !examples.containsKey(reason)) {
                examples.put(reason, state.clashExample(exam, slot));
            }
            if (reason == Constraint.CAPACITY && !examples.containsKey(reason)) {
                examples.put(reason, state.capacityExample(exam, slot));
            }
        }
        List<Rejection> blockers = counts.entrySet().stream()
                .map(entry -> new Rejection(entry.getKey(), entry.getValue(), examples.get(entry.getKey())))
                .toList();
        ExamNode node = state.exams.get(exam);
        return new Unplaced(node.id(), node.code(), blockers);
    }

    private Map<Long, Long> slotsByExam() {
        Map<Long, Long> result = new LinkedHashMap<>();
        for (int exam = 0; exam < state.exams.size(); exam++) {
            if (state.slotOf[exam] >= 0) result.put(state.exams.get(exam).id(), state.slots.get(state.slotOf[exam]).id());
        }
        return result;
    }

    private Map<Long, List<HallSeats>> hallsByExam() {
        Map<Long, List<HallSeats>> result = new TreeMap<>();
        for (int slot = 0; slot < state.slotCount(); slot++) result.putAll(state.hallPlan(slot));
        return result;
    }
}
