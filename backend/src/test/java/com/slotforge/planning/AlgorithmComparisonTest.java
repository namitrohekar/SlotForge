package com.slotforge.planning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slotforge.planning.algorithm.DsaturScheduler;
import com.slotforge.planning.algorithm.GreedyScheduler;
import com.slotforge.planning.comparison.AlgorithmComparison;
import com.slotforge.planning.comparison.AlgorithmComparison.DatasetResult;
import com.slotforge.planning.comparison.AlgorithmComparison.Measures;
import com.slotforge.planning.comparison.AlgorithmComparison.Report;
import com.slotforge.planning.comparison.AlgorithmComparison.Scenario;
import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallInfo;
import com.slotforge.planning.model.HallSeats;
import com.slotforge.planning.model.Outcome;
import com.slotforge.planning.model.PlanResult;
import com.slotforge.planning.model.Rules;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.SlotInfo;
import org.junit.jupiter.api.Test;

class AlgorithmComparisonTest {

    private static final LocalDate DAY_ZERO = LocalDate.of(2026, 11, 9);
    private static final Rules RULES = new Rules(1, 1, 500_000);
    private static final Report REPORT = AlgorithmComparison.run();

    private static SlotInfo slot(long id, int day) {
        return new SlotInfo(id, DAY_ZERO.plusDays(day), LocalTime.of(10, 0), LocalTime.of(13, 0), true);
    }

    private static ExamNode exam(long id, boolean heavy, Long... students) {
        return new ExamNode(id, "E" + id, Set.of(students), heavy, 60, null, null);
    }

    private static ExamNode exam(long id, Long... students) {
        return exam(id, false, students);
    }

    private static List<HallInfo> halls(int capacity) {
        return List.of(new HallInfo(1, "H1", capacity, Set.of()), new HallInfo(2, "H2", capacity, Set.of()), new HallInfo(3, "H3", capacity, Set.of()));
    }

    private static SchedulingProblem problem(List<ExamNode> exams, List<SlotInfo> slots, int capacity) {
        return new SchedulingProblem(exams, slots, halls(capacity), RULES);
    }

    private static HallSeats seats(long hall, int count) {
        return new HallSeats(hall, count);
    }

    private static Measures measure(SchedulingProblem problem, Map<Long, Long> slots, Map<Long, List<HallSeats>> halls) {
        return AlgorithmComparison.measure(problem, new PlanResult(Outcome.COMPLETE, slots, halls, List.of(), 0, 0), 0);
    }

    private static DatasetResult dataset(String name) {
        return REPORT.datasets().stream().filter(result -> result.name().equals(name)).findFirst().orElseThrow();
    }

    // Three matched pairs: each student sits one A and one B paper, never A1 with B1, A2 with B2 or A3 with B3.
    private static final List<ExamNode> SMALL_WORST_CASE = List.of(
            exam(1, 12L, 13L), exam(2, 21L, 31L), exam(3, 21L, 23L), exam(4, 12L, 32L), exam(5, 31L, 32L), exam(6, 13L, 23L));

    @Test
    void firstFitTakesPapersInIdOrderAndTheEarliestSlotThatWorks() {
        PlanResult plan = GreedyScheduler.schedule(problem(List.of(exam(2, 1L), exam(1, 1L), exam(3, 2L)), List.of(slot(10, 0), slot(20, 1)), 10));

        assertThat(plan.outcome()).isEqualTo(Outcome.COMPLETE);
        assertThat(plan.slotByExam()).containsExactlyInAnyOrderEntriesOf(Map.of(1L, 10L, 2L, 20L, 3L, 10L));
    }

    @Test
    void firstFitNeverGoesBackSoItCanStrandPapersDsaturPlaces() {
        SchedulingProblem problem = problem(SMALL_WORST_CASE, List.of(slot(1, 0), slot(2, 1)), 10);

        PlanResult greedy = GreedyScheduler.schedule(problem);
        PlanResult dsatur = DsaturScheduler.schedule(problem);

        assertThat(greedy.slotByExam()).containsOnlyKeys(1L, 2L, 3L, 4L);
        assertThat(greedy.backtracks()).isZero();
        assertThat(greedy.unplaced()).extracting("examId").containsExactly(5L, 6L);
        assertThat(dsatur.slotByExam()).hasSize(6);
    }

    @Test
    void neitherAlgorithmBreaksAHardRuleInAnyScenario() {
        for (DatasetResult dataset : REPORT.datasets()) {
            for (Scenario scenario : dataset.scenarios()) {
                assertThat(scenario.dsatur().ruleBreaks()).as(dataset.name() + " DSATUR, " + scenario.examDays() + " days").isZero();
                assertThat(scenario.greedy().ruleBreaks()).as(dataset.name() + " first-fit, " + scenario.examDays() + " days").isZero();
            }
        }
    }

    @Test
    void bothPlaceEveryPaperWhenThereAreDaysToSpare() {
        for (DatasetResult dataset : REPORT.datasets()) {
            Scenario roomiest = dataset.scenarios().get(0);
            assertThat(roomiest.dsatur().placed()).as(dataset.name()).isEqualTo(dataset.papers());
            assertThat(roomiest.greedy().placed()).as(dataset.name()).isEqualTo(dataset.papers());
        }
    }

    @Test
    void firstFitPlacesOnePairPerDayOnItsWorstCaseWhileDsaturPlacesAll() {
        DatasetResult worstCase = dataset("First-fit worst case");

        for (Scenario scenario : worstCase.scenarios()) {
            assertThat(scenario.greedy().placed()).isEqualTo(2 * scenario.examDays());
            assertThat(scenario.dsatur().placed()).isEqualTo(12);
        }
        assertThat(worstCase.greedyFewestDays()).isEqualTo(6);
        assertThat(worstCase.dsaturFewestDays()).isEqualTo(2);
    }

    @Test
    void repeatRunsGiveTheSameNumbers() {
        assertThat(AlgorithmComparison.run()).usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*milliseconds").isEqualTo(REPORT);
    }

    @Test
    void measuresSpacingBetweenAStudentsPapers() {
        SchedulingProblem problem = problem(List.of(exam(1, 1L), exam(2, 1L), exam(3, 1L)), List.of(slot(1, 0), slot(2, 1), slot(3, 4)), 10);

        Measures measures = measure(problem, Map.of(1L, 1L, 2L, 2L, 3L, 3L), Map.of(1L, List.of(seats(1, 1)), 2L, List.of(seats(1, 1)), 3L, List.of(seats(1, 1))));

        assertThat(measures.placed()).isEqualTo(3);
        assertThat(measures.ruleBreaks()).isZero();
        assertThat(measures.consecutiveDayPairs()).isEqualTo(1);
        assertThat(measures.averageGapDays()).isEqualTo(2.0);
    }

    @Test
    void theRecountCatchesAStudentWithTwoPapersInOneSlot() {
        SchedulingProblem problem = problem(List.of(exam(1, 1L), exam(2, 1L)), List.of(slot(1, 0)), 10);

        // Once for sharing the slot, once for going over one paper a day.
        assertThat(measure(problem, Map.of(1L, 1L, 2L, 1L), Map.of(1L, List.of(seats(1, 1)), 2L, List.of(seats(2, 1)))).ruleBreaks()).isEqualTo(2);
    }

    @Test
    void theRecountCatchesHeavyPapersTooClose() {
        SchedulingProblem problem = problem(List.of(exam(1, true, 1L), exam(2, true, 1L)), List.of(slot(1, 0), slot(2, 1)), 10);

        assertThat(measure(problem, Map.of(1L, 1L, 2L, 2L), Map.of(1L, List.of(seats(1, 1)), 2L, List.of(seats(1, 1)))).ruleBreaks()).isEqualTo(1);
    }

    @Test
    void theRecountCatchesHallProblems() {
        SchedulingProblem oneSlot = problem(List.of(exam(1, 1L), exam(2, 2L)), List.of(slot(1, 0)), 10);
        assertThat(measure(oneSlot, Map.of(1L, 1L, 2L, 1L), Map.of(1L, List.of(seats(1, 1)), 2L, List.of(seats(1, 1)))).ruleBreaks())
                .as("one hall given to two papers at once").isEqualTo(1);

        SchedulingProblem tooBig = problem(List.of(exam(1, 1L, 2L)), List.of(slot(1, 0)), 1);
        assertThat(measure(tooBig, Map.of(1L, 1L), Map.of(1L, List.of(seats(1, 2)))).ruleBreaks()).as("more seats than the hall has").isEqualTo(1);

        SchedulingProblem unseated = problem(List.of(exam(1, 1L, 2L)), List.of(slot(1, 0)), 10);
        assertThat(measure(unseated, Map.of(1L, 1L), Map.of(1L, List.of(seats(1, 1)))).ruleBreaks()).as("a student without a seat").isEqualTo(1);
    }
}
