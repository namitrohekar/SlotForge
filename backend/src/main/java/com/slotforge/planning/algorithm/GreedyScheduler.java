package com.slotforge.planning.algorithm;

import java.util.ArrayList;
import java.util.List;

import com.slotforge.planning.model.Outcome;
import com.slotforge.planning.model.PlacementStep;
import com.slotforge.planning.model.PlanResult;
import com.slotforge.planning.model.SchedulingProblem;

/**
 * The simple baseline DSATUR is measured against: papers in id order, each in the earliest slot
 * that keeps every hard rule, with no going back. It shares SearchState, so both obey the same rules.
 */
public final class GreedyScheduler {

    private GreedyScheduler() {
    }

    public static PlanResult schedule(SchedulingProblem problem) {
        SearchState state = new SearchState(problem);
        List<PlacementStep> trace = new ArrayList<>();
        for (int exam = 0; exam < state.exams.size(); exam++) {
            for (int slot = 0; slot < state.slotCount(); slot++) {
                if (state.reason(exam, slot) != null) continue;
                trace.add(state.step(exam, slot, state.legalCount(exam)));
                state.place(exam, slot);
                break;
            }
        }
        // One pass that fails proves nothing about whether a solution exists, which is what this outcome means.
        Outcome outcome = state.placed == state.exams.size() ? Outcome.COMPLETE : Outcome.BUDGET_EXHAUSTED;
        return state.result(outcome, trace.size(), 0, trace);
    }
}
