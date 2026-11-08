package com.slotforge.planning.model;

public enum Outcome {
    /** Every exam has a slot. */
    COMPLETE,
    /** The whole search tree was explored. Hall packing is a heuristic, so check blockers before calling it impossible. */
    NO_SOLUTION,
    /** The node budget ran out, which proves nothing about whether a solution exists. */
    BUDGET_EXHAUSTED
}
