package com.slotforge.planning.quality;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallInfo;
import com.slotforge.planning.model.HallSeats;
import com.slotforge.planning.model.PlanResult;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.SlotInfo;

/**
 * How good a timetable is beyond being legal. Three parts, each from 0 to 1:
 * spacing between a student's papers, how evenly duties are shared, and how well the halls in use are filled.
 */
public final class QualityScorer {

    /** score is out of 100. The other fields are the parts and the raw counts behind them. */
    public record Quality(
            double score, double spacing, double dutyBalance, double utilisation, int backToBackPairs, double averageGapDays,
            int fewestDuties, int mostDuties) {
    }

    private static final double SPACING_WEIGHT = 0.5;
    private static final double BALANCE_WEIGHT = 0.3;
    private static final double UTILISATION_WEIGHT = 0.2;
    // Indexed by days between two papers of one student: same day, next day, one day between.
    private static final double[] CROWDING = {1.0, 0.6, 0.2};

    private QualityScorer() {
    }

    /** dutiesPerPerson has one entry for everyone in the pool, including people given no duty. */
    public static Quality score(SchedulingProblem problem, PlanResult plan, Collection<Integer> dutiesPerPerson) {
        Map<Long, LocalDate> dateOfSlot = problem.slots().stream().collect(Collectors.toMap(SlotInfo::id, SlotInfo::date));
        Map<Long, List<LocalDate>> datesOf = new HashMap<>();
        for (ExamNode paper : problem.exams()) {
            Long slot = plan.slotByExam().get(paper.id());
            if (slot == null) continue;
            paper.students().forEach(student -> datesOf.computeIfAbsent(student, key -> new ArrayList<>()).add(dateOfSlot.get(slot)));
        }

        int pairs = 0;
        int backToBack = 0;
        long totalGap = 0;
        double crowding = 0;
        for (List<LocalDate> dates : datesOf.values()) {
            dates.sort(null);
            for (int index = 0; index + 1 < dates.size(); index++) {
                long gap = ChronoUnit.DAYS.between(dates.get(index), dates.get(index + 1));
                pairs++;
                totalGap += gap;
                if (gap == 1) backToBack++;
                if (gap < CROWDING.length) crowding += CROWDING[(int) gap];
            }
        }
        double spacing = pairs == 0 ? 1 : 1 - crowding / pairs;
        double balance = balance(dutiesPerPerson);
        double utilisation = utilisation(problem, plan);
        double score = 100 * (SPACING_WEIGHT * spacing + BALANCE_WEIGHT * balance + UTILISATION_WEIGHT * utilisation);
        return new Quality(round(score), round(spacing), round(balance), round(utilisation), backToBack,
                pairs == 0 ? 0 : round((double) totalGap / pairs),
                dutiesPerPerson.stream().mapToInt(Integer::intValue).min().orElse(0),
                dutiesPerPerson.stream().mapToInt(Integer::intValue).max().orElse(0));
    }

    /** 1 when everyone has the same number of duties, falling as the spread grows against the average. */
    private static double balance(Collection<Integer> duties) {
        double mean = duties.stream().mapToInt(Integer::intValue).average().orElse(0);
        if (mean == 0) return 1;
        double variance = duties.stream().mapToDouble(count -> (count - mean) * (count - mean)).sum() / duties.size();
        return Math.max(0, 1 - Math.sqrt(variance) / mean);
    }

    private static double utilisation(SchedulingProblem problem, PlanResult plan) {
        Map<Long, Integer> capacity = problem.halls().stream().collect(Collectors.toMap(HallInfo::id, HallInfo::capacity));
        long seated = 0;
        long available = 0;
        for (List<HallSeats> halls : plan.hallsByExam().values()) {
            for (HallSeats hall : halls) {
                seated += hall.seats();
                available += capacity.get(hall.hallId());
            }
        }
        return available == 0 ? 1 : (double) seated / available;
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
