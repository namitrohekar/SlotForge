package com.slotforge.planning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slotforge.planning.algorithm.DsaturScheduler;
import com.slotforge.planning.allocation.HallPacker;
import com.slotforge.planning.allocation.HallPacker.Demand;
import com.slotforge.planning.allocation.InvigilatorAllocator;
import com.slotforge.planning.allocation.InvigilatorAllocator.Duty;
import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallInfo;
import com.slotforge.planning.model.HallSeats;
import com.slotforge.planning.model.InvigilatorInfo;
import com.slotforge.planning.model.Pins;
import com.slotforge.planning.model.PlanResult;
import com.slotforge.planning.model.Rules;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.SlotInfo;
import com.slotforge.planning.repair.RepairPlanner;
import com.slotforge.planning.repair.RepairPlanner.Base;
import com.slotforge.planning.repair.RepairPlanner.Repair;
import org.junit.jupiter.api.Test;

/** Pinned papers, kept halls and invigilators, and repair that widens only when it must. */
class RepairPlannerTest {

    private static final LocalDate DAY_ZERO = LocalDate.of(2026, 12, 7);
    private static final Rules RULES = new Rules(1, 0, 500_000);

    private static SlotInfo slot(long id, int day, boolean open) {
        return new SlotInfo(id, DAY_ZERO.plusDays(day), LocalTime.of(10, 0), LocalTime.of(13, 0), open);
    }

    private static SlotInfo slot(long id, int day) {
        return slot(id, day, true);
    }

    private static ExamNode exam(long id, Long... students) {
        return new ExamNode(id, "E" + id, Set.of(students), false, 180, null, null);
    }

    private static HallInfo hall(long id, int capacity, Long... closedIn) {
        return new HallInfo(id, "H" + id, capacity, Set.of(closedIn));
    }

    private static SchedulingProblem problem(List<ExamNode> exams, List<SlotInfo> slots, HallInfo... halls) {
        return new SchedulingProblem(exams, slots, List.of(halls), RULES);
    }

    @Test
    void aPinnedPaperStaysWhereItWasEvenIfTheEngineWouldChooseElsewhere() {
        SchedulingProblem problem = problem(List.of(exam(1, 1L), exam(2, 2L)), List.of(slot(1, 0), slot(2, 1), slot(3, 2)), hall(1, 10), hall(2, 10));

        assertThat(DsaturScheduler.schedule(problem).slotByExam()).containsEntry(1L, 1L);
        assertThat(DsaturScheduler.schedule(problem, new Pins(Map.of(1L, 3L), Map.of(), Map.of())).slotByExam()).containsEntry(1L, 3L).containsEntry(2L, 1L);
    }

    @Test
    void onlyThePaperWhoseSlotClosedMovesAndItLandsOnTheNearestDate() {
        // Paper 2 was on day 4, which has closed. Days 0, 5 and 9 are open: day 5 is nearest.
        List<SlotInfo> slots = List.of(slot(1, 0), slot(2, 4, false), slot(3, 5), slot(4, 9));
        SchedulingProblem problem = problem(List.of(exam(1, 1L), exam(2, 2L), exam(3, 3L)), slots, hall(1, 10), hall(2, 10), hall(3, 10));
        Base base = new Base(Map.of(1L, 1L, 2L, 2L, 3L, 4L), Map.of());

        Repair repair = RepairPlanner.repair(problem, base, Map.of());

        assertThat(repair.level()).isEqualTo(1);
        assertThat(repair.moved()).containsExactly(2L);
        assertThat(repair.plan().slotByExam()).containsExactlyInAnyOrderEntriesOf(Map.of(1L, 1L, 2L, 3L, 3L, 4L));
    }

    @Test
    void nothingMovesWhenNothingIsWrong() {
        SchedulingProblem problem = problem(List.of(exam(1, 1L), exam(2, 1L)), List.of(slot(1, 0), slot(2, 3)), hall(1, 10));
        Repair repair = RepairPlanner.repair(problem, new Base(Map.of(1L, 2L, 2L, 1L), Map.of()), Map.of());

        assertThat(repair.level()).isZero();
        assertThat(repair.moved()).isEmpty();
        assertThat(repair.plan().slotByExam()).containsEntry(1L, 2L).containsEntry(2L, 1L);
    }

    @Test
    void repairWidensToClashingPapersOnlyWhenTheNarrowAttemptLeavesAPaperOut() {
        // Papers 1 and 2 share a student. Paper 1's slot closed. The only other slot holds paper 2, so paper 2 must give way.
        List<SlotInfo> slots = List.of(slot(1, 0, false), slot(2, 1), slot(3, 2));
        ExamNode displaced = new ExamNode(1, "E1", Set.of(1L), false, 180, null, DAY_ZERO.plusDays(1));
        SchedulingProblem problem = problem(List.of(displaced, exam(2, 1L)), slots, hall(1, 10));

        Repair repair = RepairPlanner.repair(problem, new Base(Map.of(1L, 1L, 2L, 2L), Map.of()), Map.of());

        assertThat(repair.level()).isEqualTo(2);
        assertThat(repair.plan().slotByExam()).containsEntry(1L, 2L).containsEntry(2L, 3L);
        assertThat(repair.moved()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void aPaperPlacedByHandStaysPutAtEveryLevel() {
        List<SlotInfo> slots = List.of(slot(1, 0), slot(2, 1), slot(3, 2));
        SchedulingProblem problem = problem(List.of(exam(1, 1L), exam(2, 1L)), slots, hall(1, 10));

        Repair repair = RepairPlanner.repair(problem, new Base(Map.of(1L, 1L, 2L, 2L), Map.of()), Map.of(2L, 3L));

        assertThat(repair.plan().slotByExam()).containsEntry(2L, 3L).containsEntry(1L, 1L);
        assertThat(repair.moved()).containsExactly(2L);
    }

    @Test
    void aPaperKeepsItsHallWhenAnotherPaperJoinsItsSlot() {
        // Greedy packing alone would give the 30-seat paper the small hall. It had the large one, and keeps it.
        List<Demand> demands = List.of(new Demand(1, 30), new Demand(2, 25));
        List<HallInfo> halls = List.of(hall(1, 30), hall(2, 60));

        assertThat(HallPacker.pack(demands, halls).orElseThrow().get(1L)).containsExactly(new HallSeats(1, 30));
        assertThat(HallPacker.pack(demands, halls, Map.of(1L, List.of(new HallSeats(2, 30)))).orElseThrow())
                .containsEntry(1L, List.of(new HallSeats(2, 30)))
                .containsEntry(2L, List.of(new HallSeats(1, 25)));
    }

    @Test
    void aKeptHallIsGivenUpWhenItIsGoneOrWouldLeaveAnotherPaperOut() {
        // The kept hall is closed, so the paper is packed afresh.
        assertThat(HallPacker.pack(List.of(new Demand(1, 30)), List.of(hall(1, 40)), Map.of(1L, List.of(new HallSeats(9, 30)))).orElseThrow())
                .containsEntry(1L, List.of(new HallSeats(1, 30)));
        // Keeping the 60-seat hall for the small paper would leave the 50-seat paper with nowhere, so both are repacked.
        assertThat(HallPacker.pack(List.of(new Demand(1, 20), new Demand(2, 50)), List.of(hall(1, 30), hall(2, 60)),
                Map.of(1L, List.of(new HallSeats(2, 20)))).orElseThrow())
                .containsEntry(1L, List.of(new HallSeats(1, 20)))
                .containsEntry(2L, List.of(new HallSeats(2, 50)));
    }

    @Test
    void whenOneHallOfASplitPaperIsLostOnlyThoseSeatsMove() {
        // The paper sat 50 in hall 1 and 30 in hall 2. Hall 1 is closed: its 50 go to hall 3, in the same place in the seating order.
        List<HallSeats> before = List.of(new HallSeats(1, 50), new HallSeats(2, 30));

        assertThat(HallPacker.pack(List.of(new Demand(7, 80)), List.of(hall(2, 30), hall(3, 60), hall(4, 100)), Map.of(7L, before)).orElseThrow())
                .containsEntry(7L, List.of(new HallSeats(3, 50), new HallSeats(2, 30)));
    }

    @Test
    void aKeptHallIsNeverGivenToTwoPapers() {
        Map<Long, List<HallSeats>> packed = HallPacker.pack(List.of(new Demand(1, 20), new Demand(2, 20)), List.of(hall(1, 30), hall(2, 30)),
                Map.of(1L, List.of(new HallSeats(1, 20)), 2L, List.of(new HallSeats(1, 20)))).orElseThrow();

        assertThat(packed.get(1L)).containsExactly(new HallSeats(1, 20));
        assertThat(packed.get(2L)).containsExactly(new HallSeats(2, 20));
    }

    @Test
    void invigilatorsStayOnTheirDutiesAndOnlyTheAbsentOneIsReplaced() {
        List<Duty> duties = List.of(new Duty(1, 10, 2), new Duty(2, 20, 1));
        List<InvigilatorInfo> staff = List.of(new InvigilatorInfo(1, Set.of()), new InvigilatorInfo(2, Set.of(10L)),
                new InvigilatorInfo(3, Set.of()), new InvigilatorInfo(4, Set.of()));

        Map<Long, List<Long>> assigned = InvigilatorAllocator.allocate(duties, staff, 8, Map.of(1L, List.of(4L, 2L), 2L, List.of(3L)));

        // Person 2 is away in slot 10 and is replaced by person 1, the least loaded. Persons 4 and 3 keep their duties.
        assertThat(assigned.get(1L)).containsExactly(4L, 1L);
        assertThat(assigned.get(2L)).containsExactly(3L);
    }

    @Test
    void aRepairedPlanBreaksNoRuleTheEngineKeeps() {
        // Paper 3 clashes with both others and its slot closed. One hall is closed on the last day.
        List<SlotInfo> slots = List.of(slot(1, 0), slot(2, 1, false), slot(3, 2), slot(4, 3));
        SchedulingProblem problem = problem(List.of(exam(1, 1L, 2L), exam(2, 3L, 4L), exam(3, 1L, 3L)), slots, hall(1, 2), hall(2, 2, 4L));

        PlanResult plan = RepairPlanner.repair(problem, new Base(Map.of(1L, 1L, 2L, 3L, 3L, 2L), Map.of()), Map.of()).plan();

        assertThat(plan.unplaced()).isEmpty();
        assertThat(plan.slotByExam().get(3L)).isNotIn(plan.slotByExam().get(1L), plan.slotByExam().get(2L), 2L);
        assertThat(plan.slotByExam()).containsEntry(1L, 1L).containsEntry(2L, 3L);
    }
}
