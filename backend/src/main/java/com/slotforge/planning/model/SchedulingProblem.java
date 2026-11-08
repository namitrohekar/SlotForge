package com.slotforge.planning.model;

import java.util.List;

public record SchedulingProblem(List<ExamNode> exams, List<SlotInfo> slots, List<HallInfo> halls, Rules rules) {
}
