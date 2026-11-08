package com.slotforge.planning.model;

import java.util.List;

public record Unplaced(long examId, String code, List<Rejection> blockers) {
}
