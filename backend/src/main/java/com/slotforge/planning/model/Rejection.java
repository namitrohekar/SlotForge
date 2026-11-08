package com.slotforge.planning.model;

/** How many slots refused an exam for one reason. example may be null. */
public record Rejection(Constraint constraint, int slots, String example) {
}
