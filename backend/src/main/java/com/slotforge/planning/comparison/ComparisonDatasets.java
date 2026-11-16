package com.slotforge.planning.comparison;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallInfo;
import com.slotforge.planning.model.Rules;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.SlotInfo;

/** The fixed test data both algorithms are measured on. It needs no database and is the same on every run. */
final class ComparisonDatasets {

    static final long SEARCH_LIMIT = 100_000;

    // The capacities of the ten halls in the demo data.
    private static final int[] HALL_CAPACITIES = {120, 100, 80, 80, 60, 60, 45, 45, 40, 40};
    private static final LocalDate FIRST_MONDAY = LocalDate.of(2026, 11, 16);
    private static final int PAPER_MINUTES = 180;
    private static final long CHOICE_SEED = 2026;

    record Dataset(String name, String description, List<ExamNode> papers, List<Integer> examDays) {
    }

    private record Cohort(List<Long> students, List<ExamNode> papers) {
    }

    private ComparisonDatasets() {
    }

    static List<Dataset> all() {
        return List.of(programmeCohorts(), openElectives(), firstFitWorstCase());
    }

    /** Two three-hour slots on each of the first examDays weekdays, one paper per student per day. */
    static SchedulingProblem problem(Dataset dataset, int examDays) {
        List<SlotInfo> slots = new ArrayList<>();
        for (LocalDate date = FIRST_MONDAY; slots.size() < examDays * 2; date = date.plusDays(1)) {
            if (date.getDayOfWeek().getValue() > 5) continue;
            slots.add(new SlotInfo(slots.size() + 1L, date, LocalTime.of(10, 0), LocalTime.of(13, 0), true));
            slots.add(new SlotInfo(slots.size() + 1L, date, LocalTime.of(14, 0), LocalTime.of(17, 0), true));
        }
        return new SchedulingProblem(dataset.papers(), slots, halls(), new Rules(1, 1, SEARCH_LIMIT));
    }

    static List<HallInfo> halls() {
        List<HallInfo> halls = new ArrayList<>();
        for (int capacity : HALL_CAPACITIES) halls.add(new HallInfo(halls.size() + 1L, "H" + (halls.size() + 1), capacity, Set.of()));
        return halls;
    }

    private static Dataset programmeCohorts() {
        Ids ids = new Ids();
        cohort(ids, "MCA1", 60, 5, 1);
        cohort(ids, "MCA3", 60, 5, 1);
        Set<Long> mathematics = new HashSet<>();
        List<Set<Long>> electives = List.of(new HashSet<>(), new HashSet<>(), new HashSet<>());
        String[] branches = {"CE", "IT", "EE"};
        int[] sizes = {75, 75, 60};
        for (int branch = 0; branch < branches.length; branch++) {
            Cohort third = cohort(ids, "BT" + branches[branch] + "3", sizes[branch], 5, 2);
            Cohort fifth = cohort(ids, "BT" + branches[branch] + "5", sizes[branch], 6, 2);
            mathematics.addAll(third.students());
            for (int student = 0; student < sizes[branch]; student++) {
                electives.get(student % 3).add(fifth.students().get(student));
                // Every tenth fifth-semester student also resits one third-semester paper.
                if (student % 10 == 0) third.papers().get((student / 10) % 5).students().add(fifth.students().get(student));
            }
        }
        ids.paper("MATH", true).students().addAll(mathematics);
        for (int elective = 0; elective < electives.size(); elective++) {
            ids.paper("OE" + (elective + 1), false).students().addAll(electives.get(elective));
        }
        return new Dataset("Programme cohorts",
                "Eight MCA and B.Tech cohorts sit their own papers, plus a mathematics paper shared by three branches, "
                        + "three open electives and students resitting earlier papers.",
                ids.papers, List.of(14, 12, 11, 10));
    }

    private static Cohort cohort(Ids ids, String code, int size, int paperCount, int heavy) {
        List<Long> students = ids.students(size);
        List<ExamNode> papers = new ArrayList<>();
        for (int index = 0; index < paperCount; index++) {
            ExamNode paper = ids.paper(code + "-" + (index + 1), index < heavy);
            paper.students().addAll(students);
            papers.add(paper);
        }
        return new Cohort(students, papers);
    }

    private static Dataset openElectives() {
        Ids ids = new Ids();
        for (int index = 0; index < 40; index++) ids.paper("E" + (index + 1), index % 5 == 0);
        Random random = new Random(CHOICE_SEED);
        for (int group = 0; group < 80; group++) {
            Set<Integer> choices = new HashSet<>();
            while (choices.size() < 4) choices.add(random.nextInt(40));
            List<Long> members = ids.students(5);
            for (int choice : choices) ids.papers.get(choice).students().addAll(members);
        }
        return new Dataset("Open electives",
                "Eighty groups of five students each choose four of forty papers, so who clashes with whom follows no pattern. "
                        + "The choices come from a fixed random seed.",
                ids.papers, List.of(12, 11, 10, 9, 8, 7));
    }

    /** Two days are enough, but papers entered in this order lead first-fit to use a new day for every pair. */
    private static Dataset firstFitWorstCase() {
        Ids ids = new Ids();
        int pairs = 6;
        List<ExamNode> first = new ArrayList<>();
        List<ExamNode> second = new ArrayList<>();
        for (int index = 0; index < pairs; index++) {
            first.add(ids.paper("A" + (index + 1), false));
            second.add(ids.paper("B" + (index + 1), false));
        }
        for (int a = 0; a < pairs; a++) {
            for (int b = 0; b < pairs; b++) {
                if (a == b) continue;
                long student = ids.students(1).get(0);
                first.get(a).students().add(student);
                second.get(b).students().add(student);
            }
        }
        return new Dataset("First-fit worst case",
                "Twelve papers in two groups of six. Each student sits one paper from each group, never a matching pair. "
                        + "Built to show where a simple method breaks down, so it is not typical of a real session.",
                ids.papers, List.of(6, 5, 4, 3, 2));
    }

    /** Hands out paper and student ids in the order they are added, as a database would. */
    private static final class Ids {
        final List<ExamNode> papers = new ArrayList<>();
        private long nextStudent = 1;

        ExamNode paper(String code, boolean heavy) {
            ExamNode paper = new ExamNode(papers.size() + 1L, code, new HashSet<>(), heavy, PAPER_MINUTES, null, null);
            papers.add(paper);
            return paper;
        }

        List<Long> students(int count) {
            List<Long> students = new ArrayList<>();
            for (int index = 0; index < count; index++) students.add(nextStudent++);
            return students;
        }
    }
}
