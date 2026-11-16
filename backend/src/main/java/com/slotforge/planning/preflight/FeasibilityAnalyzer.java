package com.slotforge.planning.preflight;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.slotforge.planning.allocation.InvigilatorAllocator;
import com.slotforge.planning.graph.ConflictGraph;
import com.slotforge.planning.graph.ConflictGraphBuilder;
import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallInfo;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.SlotInfo;
import com.slotforge.planning.model.Staffing;

/**
 * Finds input that cannot be scheduled before any solving is tried, and says why in plain words.
 * Every check is a proof of trouble, never a guess: passing all of them does not promise a full timetable.
 */
public final class FeasibilityAnalyzer {

    /** blocking is true when at least one paper is certain to be left out. */
    public record Finding(boolean blocking, String code, String message) {
    }

    private static final int NAMES_SHOWN = 4;

    private FeasibilityAnalyzer() {
    }

    public static List<Finding> analyse(SchedulingProblem problem) {
        List<ExamNode> papers = problem.exams().stream().sorted(Comparator.comparingLong(ExamNode::id)).toList();
        List<SlotInfo> open = problem.slots().stream().filter(SlotInfo::open).toList();
        List<Finding> findings = new ArrayList<>();
        if (papers.isEmpty() || open.isEmpty()) return findings;

        papersWithNoSlot(papers, open, findings);
        papersTooLargeForTheHalls(problem, papers, open, findings);
        studentsWithTooManyPapers(problem, papers, open, findings);
        papersThatAllClash(papers, open, findings);
        staffing(problem.staffing(), papers, findings);
        return findings;
    }

    private static void papersWithNoSlot(List<ExamNode> papers, List<SlotInfo> open, List<Finding> findings) {
        List<String> stuck = papers.stream()
                .filter(paper -> open.stream().noneMatch(slot -> fits(paper, slot)))
                .map(ExamNode::code)
                .toList();
        if (!stuck.isEmpty()) {
            findings.add(new Finding(true, "NO_SLOT_FITS", "%s: no open slot is long enough and inside %s allowed dates."
                    .formatted(named(stuck), stuck.size() == 1 ? "its" : "their")));
        }
    }

    private static boolean fits(ExamNode paper, SlotInfo slot) {
        return slot.minutes() >= paper.durationMinutes()
                && (paper.earliest() == null || !slot.date().isBefore(paper.earliest()))
                && (paper.latest() == null || !slot.date().isAfter(paper.latest()));
    }

    private static void papersTooLargeForTheHalls(SchedulingProblem problem, List<ExamNode> papers, List<SlotInfo> open, List<Finding> findings) {
        int seats = open.stream()
                .mapToInt(slot -> problem.halls().stream().filter(hall -> !hall.unavailableSlots().contains(slot.id())).mapToInt(HallInfo::capacity).sum())
                .max().orElse(0);
        for (ExamNode paper : papers) {
            if (paper.enrolled() > seats) {
                findings.add(new Finding(true, "TOO_LARGE", "%s has %d students, but all the halls free in one slot seat only %d."
                        .formatted(paper.code(), paper.enrolled(), seats)));
            }
        }
    }

    private static void studentsWithTooManyPapers(SchedulingProblem problem, List<ExamNode> papers, List<SlotInfo> open, List<Finding> findings) {
        List<LocalDate> days = open.stream().map(SlotInfo::date).distinct().sorted().toList();
        Map<Long, Integer> all = new HashMap<>();
        Map<Long, Integer> heavy = new HashMap<>();
        for (ExamNode paper : papers) {
            for (Long student : paper.students()) {
                all.merge(student, 1, Integer::sum);
                if (paper.heavy()) heavy.merge(student, 1, Integer::sum);
            }
        }
        int perDay = problem.rules().maxExamsPerStudentPerDay();
        int limit = days.size() * perDay;
        long overloaded = all.values().stream().filter(count -> count > limit).count();
        if (overloaded > 0) {
            int most = all.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            findings.add(new Finding(true, "TOO_MANY_PAPERS",
                    "%d %s more papers (up to %d) than %d exam %s at %d a day can hold. Add days or raise the daily limit."
                            .formatted(overloaded, overloaded == 1 ? "student sits" : "students sit", most, days.size(),
                                    days.size() == 1 ? "day" : "days", perDay)));
        }
        // h heavy papers, each more than gap days from the last, span at least (h - 1) * (gap + 1) + 1 calendar days.
        long span = days.get(days.size() - 1).toEpochDay() - days.get(0).toEpochDay() + 1;
        int gap = problem.rules().heavyGapDays();
        long cramped = heavy.values().stream().filter(count -> (long) (count - 1) * (gap + 1) + 1 > span).count();
        if (cramped > 0) {
            findings.add(new Finding(true, "HEAVY_PAPERS_TOO_CLOSE",
                    "%d %s more heavy papers than fit in %d days with %d rest %s between them. Add days or lower the rest days."
                            .formatted(cramped, cramped == 1 ? "student sits" : "students sit", span, gap, gap == 1 ? "day" : "days")));
        }
    }

    // Papers that all share students with each other each need a slot of their own. Found greedily, so it can only undercount.
    private static void papersThatAllClash(List<ExamNode> papers, List<SlotInfo> open, List<Finding> findings) {
        ConflictGraph graph = ConflictGraphBuilder.build(papers);
        List<Integer> byDegree = IntStream.range(0, papers.size()).boxed()
                .sorted(Comparator.comparingInt((Integer paper) -> graph.degree(paper)).reversed())
                .toList();
        List<Integer> clique = new ArrayList<>();
        for (int candidate : byDegree) {
            if (clique.stream().allMatch(member -> graph.weight(member, candidate) > 0)) clique.add(candidate);
        }
        if (clique.size() > open.size()) {
            findings.add(new Finding(true, "TOO_FEW_SLOTS",
                    "%d papers all share students with each other, so each needs its own slot, but only %d slots are open: %s."
                            .formatted(clique.size(), open.size(), named(clique.stream().map(index -> papers.get(index).code()).toList()))));
        }
    }

    private static void staffing(Staffing staffing, List<ExamNode> papers, List<Finding> findings) {
        if (!staffing.checked()) {
            findings.add(new Finding(true, "NO_INVIGILATORS", "Nobody can invigilate: no active account holds the Faculty or Invigilator role."));
            return;
        }
        int pool = staffing.invigilators().size();
        int needed = papers.stream().mapToInt(paper -> InvigilatorAllocator.needed(paper.enrolled(), staffing.studentsPerInvigilator())).sum();
        int covered = pool * staffing.maxDuties();
        if (needed > covered) {
            findings.add(new Finding(true, "TOO_FEW_INVIGILATORS",
                    "These papers need at least %d invigilation duties, but %d staff at %d duties each cover only %d."
                            .formatted(needed, pool, staffing.maxDuties(), covered)));
        }
        for (ExamNode paper : papers) {
            int forPaper = InvigilatorAllocator.needed(paper.enrolled(), staffing.studentsPerInvigilator());
            if (forPaper > pool) {
                findings.add(new Finding(true, "PAPER_NEEDS_MORE_STAFF", "%s needs %d invigilators at once, but only %d staff can invigilate."
                        .formatted(paper.code(), forPaper, pool)));
            }
        }
    }

    private static String named(List<String> codes) {
        String shown = String.join(", ", codes.subList(0, Math.min(NAMES_SHOWN, codes.size())));
        return codes.size() > NAMES_SHOWN ? shown + " and " + (codes.size() - NAMES_SHOWN) + " more" : shown;
    }
}
