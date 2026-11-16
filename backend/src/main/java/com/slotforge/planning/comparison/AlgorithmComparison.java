package com.slotforge.planning.comparison;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.slotforge.planning.algorithm.DsaturScheduler;
import com.slotforge.planning.algorithm.GreedyScheduler;
import com.slotforge.planning.comparison.ComparisonDatasets.Dataset;
import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallInfo;
import com.slotforge.planning.model.HallSeats;
import com.slotforge.planning.model.PlanResult;
import com.slotforge.planning.model.Rules;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.SlotInfo;

/** Runs DSATUR and the greedy baseline on the same data and measures both results the same way. Writes nothing. */
public final class AlgorithmComparison {

    /** ruleBreaks is recounted here from the finished timetable, not taken on trust from the algorithm. */
    public record Measures(
            int placed, int ruleBreaks, int consecutiveDayPairs, double averageGapDays, long searchSteps, long backtracks,
            double milliseconds) {
    }

    public record Scenario(int examDays, Measures dsatur, Measures greedy) {
    }

    /** The fewest-days fields are null when no tried number of days placed every paper. */
    public record DatasetResult(
            String name, String description, int papers, int students, int halls, List<Scenario> scenarios,
            Integer dsaturFewestDays, Integer greedyFewestDays) {
    }

    public record Report(long searchLimit, List<DatasetResult> datasets) {
    }

    private AlgorithmComparison() {
    }

    public static Report run() {
        return new Report(ComparisonDatasets.SEARCH_LIMIT, ComparisonDatasets.all().stream().map(AlgorithmComparison::compare).toList());
    }

    private static DatasetResult compare(Dataset dataset) {
        List<Scenario> scenarios = dataset.examDays().stream().map(days -> {
            SchedulingProblem problem = ComparisonDatasets.problem(dataset, days);
            return new Scenario(days, timed(DsaturScheduler::schedule, problem), timed(GreedyScheduler::schedule, problem));
        }).toList();
        int papers = dataset.papers().size();
        long students = dataset.papers().stream().flatMap(paper -> paper.students().stream()).distinct().count();
        return new DatasetResult(dataset.name(), dataset.description(), papers, (int) students, ComparisonDatasets.halls().size(), scenarios,
                fewestDays(scenarios, papers, Scenario::dsatur), fewestDays(scenarios, papers, Scenario::greedy));
    }

    private static Integer fewestDays(List<Scenario> scenarios, int papers, Function<Scenario, Measures> algorithm) {
        return scenarios.stream()
                .filter(scenario -> algorithm.apply(scenario).placed() == papers)
                .map(Scenario::examDays)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private static Measures timed(Function<SchedulingProblem, PlanResult> algorithm, SchedulingProblem problem) {
        long started = System.nanoTime();
        PlanResult plan = algorithm.apply(problem);
        return measure(problem, plan, (System.nanoTime() - started) / 1_000_000.0);
    }

    public static Measures measure(SchedulingProblem problem, PlanResult plan, double milliseconds) {
        Map<Long, LocalDate> dateOfSlot = problem.slots().stream().collect(Collectors.toMap(SlotInfo::id, SlotInfo::date));
        Function<ExamNode, LocalDate> dateOf = paper -> dateOfSlot.get(plan.slotByExam().get(paper.id()));
        Map<Long, List<ExamNode>> papersOf = new HashMap<>();
        for (ExamNode paper : problem.exams()) {
            if (!plan.slotByExam().containsKey(paper.id())) continue;
            paper.students().forEach(student -> papersOf.computeIfAbsent(student, key -> new ArrayList<>()).add(paper));
        }

        int ruleBreaks = hallBreaks(problem, plan);
        int consecutive = 0;
        int gaps = 0;
        long totalGap = 0;
        for (List<ExamNode> papers : papersOf.values()) {
            papers.sort(Comparator.comparing(dateOf));
            ruleBreaks += studentBreaks(papers, plan, dateOf, problem.rules());
            for (int index = 0; index + 1 < papers.size(); index++) {
                long gap = ChronoUnit.DAYS.between(dateOf.apply(papers.get(index)), dateOf.apply(papers.get(index + 1)));
                totalGap += gap;
                gaps++;
                if (gap == 1) consecutive++;
            }
        }
        return new Measures(plan.slotByExam().size(), ruleBreaks, consecutive, gaps == 0 ? 0 : (double) totalGap / gaps,
                plan.searchNodes(), plan.backtracks(), milliseconds);
    }

    private static int studentBreaks(List<ExamNode> papers, PlanResult plan, Function<ExamNode, LocalDate> dateOf, Rules rules) {
        int breaks = 0;
        for (int first = 0; first < papers.size(); first++) {
            for (int second = first + 1; second < papers.size(); second++) {
                ExamNode a = papers.get(first);
                ExamNode b = papers.get(second);
                if (plan.slotByExam().get(a.id()).equals(plan.slotByExam().get(b.id()))) breaks++;
                if (a.heavy() && b.heavy() && ChronoUnit.DAYS.between(dateOf.apply(a), dateOf.apply(b)) <= rules.heavyGapDays()) breaks++;
            }
        }
        Map<LocalDate, Long> perDay = papers.stream().collect(Collectors.groupingBy(dateOf, Collectors.counting()));
        for (long count : perDay.values()) breaks += (int) Math.max(0, count - rules.maxExamsPerStudentPerDay());
        return breaks;
    }

    private static int hallBreaks(SchedulingProblem problem, PlanResult plan) {
        Map<Long, Integer> capacity = problem.halls().stream().collect(Collectors.toMap(HallInfo::id, HallInfo::capacity));
        Set<List<Long>> booked = new HashSet<>();
        int breaks = 0;
        for (ExamNode paper : problem.exams()) {
            Long slot = plan.slotByExam().get(paper.id());
            if (slot == null) continue;
            int seated = 0;
            for (HallSeats hall : plan.hallsByExam().getOrDefault(paper.id(), List.of())) {
                seated += hall.seats();
                if (hall.seats() > capacity.get(hall.hallId())) breaks++;
                if (!booked.add(List.of(slot, hall.hallId()))) breaks++;
            }
            if (seated != paper.enrolled()) breaks++;
        }
        return breaks;
    }
}
