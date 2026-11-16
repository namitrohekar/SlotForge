package com.slotforge.planning.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.slotforge.planning.model.Constraint;
import com.slotforge.planning.model.HallSeats;
import com.slotforge.planning.model.Pins;
import com.slotforge.planning.model.SchedulingProblem;

/** For one paper of an existing timetable: every slot, and the rule that keeps the paper out of it, if any. */
public final class SlotAdvisor {

    /** reason is null when the paper may move here. example adds the detail a person needs, and may be null. */
    public record Candidate(long slotId, Constraint reason, String example) {
    }

    private SlotAdvisor() {
    }

    /** placed is where every paper is now. The paper asked about is lifted out, so its own slot shows as allowed. */
    public static List<Candidate> candidates(SchedulingProblem problem, Map<Long, Long> placed, Map<Long, List<HallSeats>> halls, long examId) {
        SearchState state = new SearchState(problem, new Pins(Map.of(), halls, Map.of()));
        int target = -1;
        for (int exam = 0; exam < state.exams.size(); exam++) {
            long id = state.exams.get(exam).id();
            if (id == examId) {
                target = exam;
                continue;
            }
            Long slot = placed.get(id);
            if (slot != null && state.slotIndex(slot) >= 0) state.place(exam, state.slotIndex(slot));
        }
        List<Candidate> candidates = new ArrayList<>();
        if (target < 0) return candidates;
        for (int slot = 0; slot < state.slotCount(); slot++) {
            Constraint reason = state.reason(target, slot);
            String example = reason == Constraint.STUDENT_CLASH ? state.clashExample(target, slot)
                    : reason == Constraint.CAPACITY ? state.capacityExample(target, slot)
                    : reason == Constraint.STAFFING ? state.staffingExample(target, slot) : null;
            candidates.add(new Candidate(state.slots.get(slot).id(), reason, example));
        }
        return candidates;
    }
}
