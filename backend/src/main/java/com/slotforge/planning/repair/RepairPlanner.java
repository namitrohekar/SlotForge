package com.slotforge.planning.repair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slotforge.planning.algorithm.DsaturScheduler;
import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallSeats;
import com.slotforge.planning.model.Pins;
import com.slotforge.planning.model.PlanResult;
import com.slotforge.planning.model.SchedulingProblem;

/**
 * Rebuilds a timetable from an earlier one, changing as little as it can.
 * It widens what may move only when the narrower attempt leaves a paper out:
 * first only papers that can no longer stay, then the papers that share their students, then everything.
 */
public final class RepairPlanner {

    /** Where every paper was, and the halls it had. */
    public record Base(Map<Long, Long> slotByExam, Map<Long, List<HallSeats>> hallsByExam) {
    }

    /** level 0: nothing moved. 1: only papers that had to. 2: also the papers clashing with them. 3: rebuilt from scratch. */
    public record Repair(PlanResult plan, int level, Set<Long> moved) {
    }

    private RepairPlanner() {
    }

    /** mustStay maps a paper to the slot it has to keep at every level, such as one a coordinator placed by hand. */
    public static Repair repair(SchedulingProblem problem, Base base, Map<Long, Long> mustStay) {
        Map<Long, Long> everything = new HashMap<>(base.slotByExam());
        everything.putAll(mustStay);
        PlanResult narrow = attempt(problem, base, everything);
        if (narrow.unplaced().isEmpty()) return repaired(narrow, base, 1);

        Map<Long, Long> withoutNeighbours = new HashMap<>(everything);
        withoutNeighbours.keySet().removeAll(leftOutAndTheirNeighbours(problem, narrow));
        withoutNeighbours.putAll(mustStay);
        PlanResult wider = attempt(problem, base, withoutNeighbours);
        if (wider.unplaced().isEmpty()) return repaired(wider, base, 2);

        PlanResult widest = attempt(problem, base, mustStay);
        PlanResult best = widest.slotByExam().size() > Math.max(narrow.slotByExam().size(), wider.slotByExam().size()) ? widest
                : wider.slotByExam().size() > narrow.slotByExam().size() ? wider : narrow;
        return repaired(best, base, best == widest ? 3 : best == wider ? 2 : 1);
    }

    private static PlanResult attempt(SchedulingProblem problem, Base base, Map<Long, Long> fixed) {
        return DsaturScheduler.schedule(problem, new Pins(fixed, base.hallsByExam(), base.slotByExam()));
    }

    private static Set<Long> leftOutAndTheirNeighbours(SchedulingProblem problem, PlanResult plan) {
        Set<Long> leftOut = new HashSet<>();
        plan.unplaced().forEach(paper -> leftOut.add(paper.examId()));
        Set<Long> students = new HashSet<>();
        for (ExamNode paper : problem.exams()) {
            if (leftOut.contains(paper.id())) students.addAll(paper.students());
        }
        Set<Long> released = new HashSet<>(leftOut);
        for (ExamNode paper : problem.exams()) {
            if (paper.students().stream().anyMatch(students::contains)) released.add(paper.id());
        }
        return released;
    }

    private static Repair repaired(PlanResult plan, Base base, int level) {
        Set<Long> moved = new HashSet<>();
        base.slotByExam().forEach((exam, slot) -> {
            if (!slot.equals(plan.slotByExam().get(exam))) moved.add(exam);
        });
        return new Repair(plan, moved.isEmpty() ? 0 : level, moved);
    }
}
