package com.slotforge.planning.model;

import java.util.List;

/** Who can invigilate and how many are needed. An empty pool means staffing is not checked, as in pure algorithm tests. */
public record Staffing(List<InvigilatorInfo> invigilators, int studentsPerInvigilator, int maxDuties) {

    public static Staffing unchecked() {
        return new Staffing(List.of(), 1, 1);
    }

    public boolean checked() {
        return !invigilators.isEmpty();
    }
}
