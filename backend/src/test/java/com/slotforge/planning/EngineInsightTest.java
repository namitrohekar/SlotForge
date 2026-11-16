package com.slotforge.planning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slotforge.planning.algorithm.DsaturScheduler;
import com.slotforge.planning.model.Constraint;
import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallInfo;
import com.slotforge.planning.model.HallSeats;
import com.slotforge.planning.model.InvigilatorInfo;
import com.slotforge.planning.model.Outcome;
import com.slotforge.planning.model.PlacementStep;
import com.slotforge.planning.model.PlanResult;
import com.slotforge.planning.model.Rules;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.SlotInfo;
import com.slotforge.planning.model.Staffing;
import com.slotforge.planning.preflight.FeasibilityAnalyzer;
import com.slotforge.planning.preflight.FeasibilityAnalyzer.Finding;
import com.slotforge.planning.quality.QualityScorer;
import com.slotforge.planning.quality.QualityScorer.Quality;
import org.junit.jupiter.api.Test;

/** Staff-aware slot choice, the placement trace, the quality score and the preflight checks. */
class EngineInsightTest {

    private static final LocalDate DAY_ZERO = LocalDate.of(2026, 12, 7);
    private static final Rules RULES = new Rules(1, 1, 500_000);

    private static SlotInfo slot(long id, int day) {
        return new SlotInfo(id, DAY_ZERO.plusDays(day), LocalTime.of(10, 0), LocalTime.of(13, 0), true);
    }

    private static ExamNode exam(long id, boolean heavy, int minutes, Long... students) {
        return new ExamNode(id, "E" + id, Set.of(students), heavy, minutes, null, null);
    }

    private static ExamNode exam(long id, Long... students) {
        return exam(id, false, 180, students);
    }

    private static List<HallInfo> halls(int capacity) {
        return List.of(new HallInfo(1, "H1", capacity, Set.of()), new HallInfo(2, "H2", capacity, Set.of()), new HallInfo(3, "H3", capacity, Set.of()));
    }

    private static Staffing staff(int studentsEach, int maxDuties, InvigilatorInfo... people) {
        return new Staffing(List.of(people), studentsEach, maxDuties);
    }

    private static InvigilatorInfo person(long id, Long... awayIn) {
        return new InvigilatorInfo(id, Set.of(awayIn));
    }

    private static List<String> codes(List<Finding> findings) {
        return findings.stream().map(Finding::code).toList();
    }

    @Test
    void aPaperGoesToTheSlotWhereEnoughStaffAreFree() {
        // Four students at two per invigilator need two people. Only the second slot has two free.
        SchedulingProblem problem = new SchedulingProblem(List.of(exam(1, 1L, 2L, 3L, 4L)), List.of(slot(1, 0), slot(2, 1)), halls(10), RULES,
                staff(2, 8, person(11, 1L), person(12)));

        assertThat(DsaturScheduler.schedule(problem).slotByExam()).containsEntry(1L, 2L);
    }

    @Test
    void aPaperNobodyCanInvigilateIsLeftOutAndSaysWhy() {
        SchedulingProblem problem = new SchedulingProblem(List.of(exam(1, 1L, 2L, 3L, 4L)), List.of(slot(1, 0), slot(2, 1)), halls(10), RULES,
                staff(2, 8, person(11, 1L, 2L), person(12, 1L, 2L)));

        PlanResult plan = DsaturScheduler.schedule(problem);

        assertThat(plan.slotByExam()).isEmpty();
        assertThat(plan.unplaced().get(0).blockers()).singleElement().satisfies(blocker -> {
            assertThat(blocker.constraint()).isEqualTo(Constraint.STAFFING);
            assertThat(blocker.slots()).isEqualTo(2);
            assertThat(blocker.example()).isEqualTo("needs 2 invigilators in that slot, 0 free");
        });
    }

    @Test
    void theTraceShowsTheMostCorneredPaperGoingNext() {
        // Clashes: 1-2, 1-3, 2-3, 3-4, 4-5. Paper 3 clashes with most, then its neighbours fill up.
        List<ExamNode> papers = List.of(exam(1, 1L, 2L), exam(2, 1L, 3L), exam(3, 2L, 3L, 4L), exam(4, 4L, 5L), exam(5, 5L));
        PlanResult plan = DsaturScheduler.schedule(new SchedulingProblem(papers, List.of(slot(1, 0), slot(2, 2), slot(3, 4)), halls(10), RULES));

        assertThat(plan.outcome()).isEqualTo(Outcome.COMPLETE);
        assertThat(plan.trace()).extracting(PlacementStep::examId).containsExactly(3L, 1L, 2L, 4L, 5L);
        assertThat(plan.trace()).extracting(PlacementStep::saturation).containsExactly(0, 1, 2, 1, 1);
        assertThat(plan.trace().get(0).conflicts()).isEqualTo(3);
        assertThat(plan.trace().get(0).legalSlots()).isEqualTo(3);
        assertThat(plan.trace().get(2).legalSlots()).as("the third paper has one slot left").isEqualTo(1);
    }

    @Test
    void qualityWeighsSpacingDutySharingAndHallUse() {
        // One student, two papers on consecutive days: spacing 0.4. Equal duties: 1. Two seats used of twenty: 0.1.
        SchedulingProblem problem = new SchedulingProblem(List.of(exam(1, 1L), exam(2, 1L)), List.of(slot(1, 0), slot(2, 1)), halls(10), RULES);
        PlanResult plan = new PlanResult(Outcome.COMPLETE, Map.of(1L, 1L, 2L, 2L),
                Map.of(1L, List.of(new HallSeats(1, 1)), 2L, List.of(new HallSeats(1, 1))), List.of(), 0, 0);

        Quality quality = QualityScorer.score(problem, plan, List.of(1, 1));

        assertThat(quality.spacing()).isEqualTo(0.4);
        assertThat(quality.dutyBalance()).isEqualTo(1.0);
        assertThat(quality.utilisation()).isEqualTo(0.1);
        assertThat(quality.score()).isEqualTo(52.0);
        assertThat(quality.backToBackPairs()).isEqualTo(1);
        assertThat(quality.averageGapDays()).isEqualTo(1.0);
    }

    @Test
    void unevenDutiesLowerTheBalance() {
        SchedulingProblem problem = new SchedulingProblem(List.of(exam(1, 1L)), List.of(slot(1, 0)), halls(10), RULES);
        PlanResult plan = new PlanResult(Outcome.COMPLETE, Map.of(1L, 1L), Map.of(1L, List.of(new HallSeats(1, 1))), List.of(), 0, 0);

        Quality quality = QualityScorer.score(problem, plan, List.of(4, 0));

        assertThat(quality.dutyBalance()).isZero();
        assertThat(quality.fewestDuties()).isZero();
        assertThat(quality.mostDuties()).isEqualTo(4);
    }

    @Test
    void aSessionThatCanBeScheduledRaisesNothing() {
        SchedulingProblem problem = new SchedulingProblem(List.of(exam(1, 1L), exam(2, 1L)), List.of(slot(1, 0), slot(2, 2)), halls(10), RULES,
                staff(30, 8, person(11)));

        assertThat(FeasibilityAnalyzer.analyse(problem)).isEmpty();
    }

    @Test
    void preflightNamesEachKindOfImpossibleInput() {
        InvigilatorInfo one = person(11);
        List<SlotInfo> twoDays = List.of(slot(1, 0), slot(2, 1));

        assertThat(codes(FeasibilityAnalyzer.analyse(new SchedulingProblem(List.of(exam(1, false, 240, 1L)), twoDays, halls(10), RULES, staff(30, 8, one)))))
                .containsExactly("NO_SLOT_FITS");
        assertThat(codes(FeasibilityAnalyzer.analyse(new SchedulingProblem(List.of(exam(1, 1L, 2L, 3L, 4L)), twoDays, halls(1), RULES, staff(30, 8, one)))))
                .containsExactly("TOO_LARGE");
        assertThat(codes(FeasibilityAnalyzer.analyse(new SchedulingProblem(List.of(exam(1, 1L), exam(2, 1L), exam(3, 1L)), twoDays, halls(10), RULES, staff(30, 8, one)))))
                .contains("TOO_MANY_PAPERS", "TOO_FEW_SLOTS");
        assertThat(codes(FeasibilityAnalyzer.analyse(new SchedulingProblem(List.of(exam(1, true, 180, 1L), exam(2, true, 180, 1L)), twoDays, halls(10), RULES, staff(30, 8, one)))))
                .containsExactly("HEAVY_PAPERS_TOO_CLOSE");
        // Three papers that pairwise share a different student: nobody is overloaded, but two slots cannot hold them.
        assertThat(codes(FeasibilityAnalyzer.analyse(new SchedulingProblem(List.of(exam(1, 1L, 2L), exam(2, 1L, 3L), exam(3, 2L, 3L)), twoDays, halls(10), RULES, staff(30, 8, one)))))
                .containsExactly("TOO_FEW_SLOTS");
    }

    @Test
    void preflightChecksThereAreEnoughInvigilators() {
        List<SlotInfo> slots = List.of(slot(1, 0), slot(2, 2));
        List<ExamNode> papers = List.of(exam(1, 1L, 2L, 3L, 4L), exam(2, 5L, 6L, 7L, 8L));

        assertThat(codes(FeasibilityAnalyzer.analyse(new SchedulingProblem(papers, slots, halls(10), RULES)))).containsExactly("NO_INVIGILATORS");
        // Two per invigilator means two duties a paper, four in all, but one person may take only three.
        assertThat(codes(FeasibilityAnalyzer.analyse(new SchedulingProblem(papers, slots, halls(10), RULES, staff(2, 3, person(11))))))
                .containsExactly("TOO_FEW_INVIGILATORS", "PAPER_NEEDS_MORE_STAFF", "PAPER_NEEDS_MORE_STAFF");
        assertThat(FeasibilityAnalyzer.analyse(new SchedulingProblem(papers, slots, halls(10), RULES, staff(2, 2, person(11), person(12))))).isEmpty();
    }
}
