package com.slotforge.planning.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.slotforge.planning.model.ExamNode;

public final class ConflictGraphBuilder {

    private ConflictGraphBuilder() {
    }

    /** Exams must be sorted by id, and ids must be unique. Each student adds one to every pair of their exams. */
    public static ConflictGraph build(List<ExamNode> exams) {
        Map<Long, List<Integer>> examsByStudent = new HashMap<>();
        for (int index = 0; index < exams.size(); index++) {
            if (index > 0 && exams.get(index - 1).id() >= exams.get(index).id()) {
                throw new IllegalArgumentException("Exams must be sorted by unique id");
            }
            for (long student : exams.get(index).students()) {
                examsByStudent.computeIfAbsent(student, ignored -> new ArrayList<>()).add(index);
            }
        }

        Map<Long, Integer> weights = new HashMap<>();
        for (List<Integer> shared : examsByStudent.values()) {
            for (int a = 0; a < shared.size(); a++) {
                for (int b = a + 1; b < shared.size(); b++) {
                    weights.merge(ConflictGraph.key(shared.get(a), shared.get(b), exams.size()), 1, Integer::sum);
                }
            }
        }
        return new ConflictGraph(exams.size(), weights);
    }
}
