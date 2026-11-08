package com.slotforge.planning.model;

/** Why a slot cannot take an exam. */
public enum Constraint {
    SLOT_CLOSED, DURATION, WINDOW, STUDENT_CLASH, DAILY_LIMIT, HEAVY_GAP, CAPACITY
}
