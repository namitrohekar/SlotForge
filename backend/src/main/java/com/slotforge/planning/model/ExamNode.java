package com.slotforge.planning.model;

import java.time.LocalDate;
import java.util.Set;

/** One paper to schedule: a vertex of the conflict graph. earliest and latest may be null. */
public record ExamNode(
        long id, String code, Set<Long> students, boolean heavy, int durationMinutes, LocalDate earliest, LocalDate latest) {

    public int enrolled() {
        return students.size();
    }
}
