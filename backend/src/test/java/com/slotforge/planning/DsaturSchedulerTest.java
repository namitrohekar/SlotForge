package com.slotforge.planning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.slotforge.planning.algorithm.DsaturScheduler;
import com.slotforge.planning.model.Constraint;
import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallInfo;
import com.slotforge.planning.model.HallSeats;
import com.slotforge.planning.model.Outcome;
import com.slotforge.planning.model.PlanResult;
import com.slotforge.planning.model.Rules;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.SlotInfo;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

class DsaturSchedulerTest {

    private static final LocalDate DAY_ZERO = LocalDate.of(2026, 11, 9);
    private static final LocalTime TEN = LocalTime.of(10, 0);
    private static final Rules RULES = new Rules(1, 1, 500_000);

    private static SlotInfo slot(long id, int day, int minutes) {
        return new SlotInfo(id, DAY_ZERO.plusDays(day), TEN, TEN.plusMinutes(minutes), true);
    }

    private static SlotInfo slot(long id, int day) {
        return slot(id, day, 180);
    }

    private static Set<Long> students(long... ids) {
        return LongStream.of(ids).boxed().collect(Collectors.toSet());
    }

    private static ExamNode exam(long id, long... studentIds) {
        return new ExamNode(id, "E" + id, students(studentIds), false, 60, null, null);
    }

    private static ExamNode heavy(long id, long... studentIds) {
        return new ExamNode(id, "E" + id, students(studentIds), true, 60, null, null);
    }

    private static HallInfo hall(long id, int capacity) {
        return new HallInfo(id, "H" + id, capacity, Set.of());
    }

    private static List<HallInfo> halls(int count) {
        List<HallInfo> halls = new ArrayList<>();
        for (long id = 1; id <= count; id++) halls.add(hall(id, 1000));
        return halls;
    }

    private static PlanResult run(List<ExamNode> exams, List<SlotInfo> slots, List<HallInfo> halls, Rules rules) {
        return DsaturScheduler.schedule(new SchedulingProblem(exams, slots, halls, rules));
    }

    private static PlanResult run(List<ExamNode> exams, List<SlotInfo> slots) {
        return run(exams, slots, halls(10), RULES);
    }

    /** Every pair of the n exams shares its own student. */
    private static List<ExamNode> completeGraph(int n) {
        List<ExamNode> exams = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Set<Long> students = new HashSet<>();
            for (int j = 1; j <= n; j++) {
                if (i != j) students.add((long) Math.min(i, j) * 100 + Math.max(i, j));
            }
            exams.add(new ExamNode(i, "E" + i, students, false, 60, null, null));
        }
        return exams;
    }

    private static List<SlotInfo> weeklySlots(int count) {
        List<SlotInfo> slots = new ArrayList<>();
        for (int index = 0; index < count; index++) slots.add(slot(index + 1, index * 7));
        return slots;
    }

    @Test
    void anEmptySessionIsComplete() {
        PlanResult result = run(List.of(), weeklySlots(2));

        assertThat(result.outcome()).isEqualTo(Outcome.COMPLETE);
        assertThat(result.slotByExam()).isEmpty();
    }

    @Test
    void aSingleExamTakesTheFirstSlotAndAHall() {
        PlanResult result = run(List.of(exam(1, 10)), weeklySlots(2));

        assertThat(result.slotByExam()).containsEntry(1L, 1L);
        assertThat(result.hallsByExam().get(1L)).hasSize(1);
    }

    @Test
    void examsWithNoSharedStudentsShareASlotWhileHallsLast() {
        PlanResult result = run(List.of(exam(1, 10), exam(2, 20), exam(3, 30)), weeklySlots(1), halls(3), RULES);

        assertThat(result.outcome()).isEqualTo(Outcome.COMPLETE);
        assertThat(result.slotByExam().values()).containsOnly(1L);
    }

    // The worked example in section 11 of the plan, step for step.
    @Test
    void reproducesTheWorkedExample() {
        List<ExamNode> exams = List.of(
                exam(1, 1, 2, 6), exam(2, 1, 3), exam(3, 2, 3, 4), exam(4, 4, 5), exam(5, 5));

        PlanResult result = run(exams, weeklySlots(3));

        assertThat(result.outcome()).isEqualTo(Outcome.COMPLETE);
        assertThat(result.slotByExam()).containsOnly(
                Map.entry(1L, 2L), Map.entry(2L, 3L), Map.entry(3L, 1L), Map.entry(4L, 2L), Map.entry(5L, 1L));
        assertThat(result.backtracks()).isZero();
    }

    @Test
    void aCompleteGraphUsesEverySlot() {
        PlanResult result = run(completeGraph(4), weeklySlots(4));

        assertThat(result.outcome()).isEqualTo(Outcome.COMPLETE);
        assertThat(new HashSet<>(result.slotByExam().values())).hasSize(4);
    }

    @Test
    void tooFewSlotsLeaveOneExamOutAndSayWhy() {
        PlanResult result = run(completeGraph(4), weeklySlots(3));

        assertThat(result.outcome()).isEqualTo(Outcome.NO_SOLUTION);
        assertThat(result.slotByExam()).hasSize(3);
        assertThat(result.unplaced()).hasSize(1);
        var blocker = result.unplaced().get(0).blockers().get(0);
        assertThat(blocker.constraint()).isEqualTo(Constraint.STUDENT_CLASH);
        assertThat(blocker.slots()).isEqualTo(3);
        assertThat(blocker.example()).contains("1 shared students");
    }

    @Test
    void theBudgetStopsTheSearchAndKeepsPartialWork() {
        PlanResult result = run(completeGraph(4), weeklySlots(3), halls(10), new Rules(1, 1, 1));

        assertThat(result.outcome()).isEqualTo(Outcome.BUDGET_EXHAUSTED);
        assertThat(result.searchNodes()).isEqualTo(1);
        assertThat(result.slotByExam()).hasSize(3);
    }

    // A first takes S1, which forward checking accepts because B and C each still see S3.
    // They cannot both have it, so the search must come back and move A to S2.
    @Test
    void backtracksOutOfAChoiceThatLooksFine() {
        List<SlotInfo> slots = List.of(slot(1, 0), slot(2, 7, 120), slot(3, 14));
        List<ExamNode> exams = List.of(
                new ExamNode(1, "A", students(1, 2, 4), false, 60, null, DAY_ZERO.plusDays(7)),
                new ExamNode(2, "B", students(1, 3), false, 180, null, null),
                new ExamNode(3, "C", students(2, 3), false, 180, null, null));

        PlanResult result = run(exams, slots);

        assertThat(result.outcome()).isEqualTo(Outcome.COMPLETE);
        assertThat(result.backtracks()).isPositive();
        assertThat(result.slotByExam()).containsEntry(1L, 2L).containsEntry(2L, 1L).containsEntry(3L, 3L);
    }

    @Test
    void theDailyLimitKeepsAStudentsPapersApart() {
        List<SlotInfo> sameDay = List.of(slot(1, 0), new SlotInfo(2, DAY_ZERO, LocalTime.of(14, 0), LocalTime.of(17, 0), true));
        List<ExamNode> exams = List.of(exam(1, 10), exam(2, 10));

        PlanResult limited = run(exams, sameDay);
        PlanResult relaxed = run(exams, sameDay, halls(10), new Rules(2, 1, 500_000));

        assertThat(limited.unplaced()).hasSize(1);
        assertThat(limited.unplaced().get(0).blockers()).extracting(b -> b.constraint())
                .contains(Constraint.DAILY_LIMIT);
        assertThat(relaxed.outcome()).isEqualTo(Outcome.COMPLETE);
    }

    @Test
    void heavyPapersKeepTheirRestDay() {
        PlanResult result = run(List.of(heavy(1, 10), heavy(2, 10)), List.of(slot(1, 0), slot(2, 1), slot(3, 2)));

        assertThat(result.slotByExam()).containsEntry(1L, 1L).containsEntry(2L, 3L);
    }

    // One batch, five papers, Mon to Fri then Mon and Tue. Placing one at a time alone gives day 3 and 4 back to back.
    @Test
    void looksAgainOnceEveryPaperIsPlacedAndSpreadsThemBetter() {
        List<SlotInfo> slots = List.of(slot(1, 0), slot(2, 1), slot(3, 2), slot(4, 3), slot(5, 4), slot(6, 7), slot(7, 8));
        long[] batch = LongStream.rangeClosed(1, 75).toArray();
        List<ExamNode> exams = LongStream.rangeClosed(1, 5).mapToObj(id -> exam(id, batch)).toList();

        PlanResult result = run(exams, slots);

        assertThat(result.slotByExam()).containsOnly(
                Map.entry(1L, 1L), Map.entry(2L, 3L), Map.entry(3L, 6L), Map.entry(4L, 5L), Map.entry(5L, 7L));
    }

    @Test
    void anExamBiggerThanTheHallsSaysSo() {
        long[] crowd = LongStream.rangeClosed(1, 300).toArray();

        PlanResult result = run(List.of(exam(1, crowd)), weeklySlots(1), List.of(hall(1, 200)), RULES);

        assertThat(result.outcome()).isEqualTo(Outcome.NO_SOLUTION);
        var blocker = result.unplaced().get(0).blockers().get(0);
        assertThat(blocker.constraint()).isEqualTo(Constraint.CAPACITY);
        assertThat(blocker.example()).isEqualTo("needs 300 seats, 200 free");
    }

    @Test
    void aLargeExamSplitsAcrossHalls() {
        long[] crowd = LongStream.rangeClosed(1, 150).toArray();

        PlanResult result = run(List.of(exam(1, crowd)), weeklySlots(1), List.of(hall(1, 100), hall(2, 100)), RULES);

        assertThat(result.hallsByExam().get(1L)).extracting(HallSeats::seats).containsExactly(100, 50);
    }

    @Test
    void aHallOutOfServiceInThatSlotIsNotUsed() {
        List<HallInfo> halls = List.of(new HallInfo(1, "H1", 50, Set.of(1L)), hall(2, 500));

        PlanResult result = run(List.of(exam(1, 10)), weeklySlots(1), halls, RULES);

        assertThat(result.hallsByExam().get(1L)).containsExactly(new HallSeats(2, 1));
    }

    @Test
    void noSlotsMeansNothingIsPlaced() {
        PlanResult result = run(List.of(exam(1, 10)), List.of());

        assertThat(result.outcome()).isEqualTo(Outcome.NO_SOLUTION);
        assertThat(result.unplaced()).hasSize(1);
    }

    @RepeatedTest(200)
    void randomInstancesNeverBreakAHardRule(RepetitionInfo repetition) {
        SchedulingProblem problem = randomProblem(new Random(repetition.getCurrentRepetition()));

        PlanResult first = DsaturScheduler.schedule(problem);

        assertObeysEveryRule(problem, first);
        assertThat(DsaturScheduler.schedule(problem)).as("same input, same timetable").isEqualTo(first);
    }

    private static SchedulingProblem randomProblem(Random random) {
        List<SlotInfo> slots = new ArrayList<>();
        int slotCount = 4 + random.nextInt(12);
        for (int index = 0; index < slotCount; index++) {
            LocalDate date = DAY_ZERO.plusDays(index / 2);
            LocalTime start = index % 2 == 0 ? TEN : LocalTime.of(14, 0);
            slots.add(new SlotInfo(index + 1, date, start, start.plusMinutes(random.nextBoolean() ? 180 : 120), random.nextInt(10) > 0));
        }

        int examCount = 1 + random.nextInt(25);
        List<Set<Long>> enrolment = new ArrayList<>();
        for (int exam = 0; exam < examCount; exam++) enrolment.add(new HashSet<>());
        int studentCount = 20 + random.nextInt(180);
        for (long student = 1; student <= studentCount; student++) {
            int papers = 1 + random.nextInt(5);
            for (int paper = 0; paper < papers; paper++) enrolment.get(random.nextInt(examCount)).add(student);
        }

        List<ExamNode> exams = new ArrayList<>();
        for (int exam = 0; exam < examCount; exam++) {
            int duration = 60 * (1 + random.nextInt(3));
            LocalDate latest = random.nextInt(4) == 0 ? DAY_ZERO.plusDays(random.nextInt(8)) : null;
            exams.add(new ExamNode(exam + 1, "E" + (exam + 1), enrolment.get(exam), random.nextInt(4) == 0, duration, null, latest));
        }

        List<HallInfo> halls = new ArrayList<>();
        int hallCount = 2 + random.nextInt(7);
        for (int hall = 0; hall < hallCount; hall++) {
            Set<Long> closed = random.nextInt(3) == 0 ? Set.of(1L + random.nextInt(slotCount)) : Set.of();
            halls.add(new HallInfo(hall + 1, "H" + (hall + 1), 30 + random.nextInt(170), closed));
        }
        return new SchedulingProblem(exams, slots, halls, new Rules(1 + random.nextInt(2), random.nextInt(3), 2_000));
    }

    // Deliberately shares no code with the engine: it reads only the problem and the result.
    private static void assertObeysEveryRule(SchedulingProblem problem, PlanResult result) {
        Map<Long, ExamNode> exams = problem.exams().stream().collect(Collectors.toMap(ExamNode::id, e -> e));
        Map<Long, SlotInfo> slots = problem.slots().stream().collect(Collectors.toMap(SlotInfo::id, s -> s));
        Map<Long, HallInfo> halls = problem.halls().stream().collect(Collectors.toMap(HallInfo::id, h -> h));

        assertThat(result.slotByExam().size() + result.unplaced().size()).isEqualTo(problem.exams().size());
        Map<Long, List<Long>> papersByStudent = new HashMap<>();
        Set<String> hallBookings = new HashSet<>();

        result.slotByExam().forEach((examId, slotId) -> {
            ExamNode exam = exams.get(examId);
            SlotInfo slot = slots.get(slotId);
            assertThat(slot.open()).as("slot is open").isTrue();
            assertThat(slot.minutes()).as("duration fits").isGreaterThanOrEqualTo(exam.durationMinutes());
            if (exam.latest() != null) assertThat(slot.date()).as("inside window").isBeforeOrEqualTo(exam.latest());
            exam.students().forEach(student -> papersByStudent.computeIfAbsent(student, s -> new ArrayList<>()).add(examId));

            int seated = 0;
            for (HallSeats seats : result.hallsByExam().get(examId)) {
                HallInfo hall = halls.get(seats.hallId());
                assertThat(seats.seats()).as("seats fit the hall").isLessThanOrEqualTo(hall.capacity());
                assertThat(hall.unavailableSlots()).as("hall is available").doesNotContain(slotId);
                assertThat(hallBookings.add(hall.id() + "@" + slotId)).as("one exam per hall per slot").isTrue();
                seated += seats.seats();
            }
            assertThat(seated).as("every student has a seat").isEqualTo(exam.enrolled());
        });

        papersByStudent.forEach((student, papers) -> {
            Map<LocalDate, Integer> perDay = new HashMap<>();
            for (int a = 0; a < papers.size(); a++) {
                SlotInfo slotA = slots.get(result.slotByExam().get(papers.get(a)));
                perDay.merge(slotA.date(), 1, Integer::sum);
                for (int b = a + 1; b < papers.size(); b++) {
                    SlotInfo slotB = slots.get(result.slotByExam().get(papers.get(b)));
                    assertThat(slotA.id()).as("no student clash").isNotEqualTo(slotB.id());
                    if (exams.get(papers.get(a)).heavy() && exams.get(papers.get(b)).heavy()) {
                        long apart = Math.abs(slotA.date().toEpochDay() - slotB.date().toEpochDay());
                        assertThat(apart).as("heavy gap").isGreaterThan(problem.rules().heavyGapDays());
                    }
                }
            }
            assertThat(perDay.values()).as("daily limit").allMatch(count -> count <= problem.rules().maxExamsPerStudentPerDay());
        });
    }
}
