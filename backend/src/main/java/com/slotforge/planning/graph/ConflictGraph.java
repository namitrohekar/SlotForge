package com.slotforge.planning.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Exams are vertices, numbered by position in the id-sorted list. An edge's weight is the students they share. */
public final class ConflictGraph {

    private final int size;
    private final int[][] neighbours;
    private final Map<Long, Integer> weights;

    ConflictGraph(int size, Map<Long, Integer> weights) {
        this.size = size;
        this.weights = weights;
        List<List<Integer>> adjacent = new ArrayList<>();
        for (int exam = 0; exam < size; exam++) adjacent.add(new ArrayList<>());
        for (long key : weights.keySet()) {
            int low = (int) (key / size);
            int high = (int) (key % size);
            adjacent.get(low).add(high);
            adjacent.get(high).add(low);
        }
        // Sorted because the map's iteration order must never reach the search.
        neighbours = adjacent.stream()
                .map(list -> list.stream().mapToInt(Integer::intValue).sorted().toArray())
                .toArray(int[][]::new);
    }

    static long key(int low, int high, int size) {
        return (long) low * size + high;
    }

    public int size() {
        return size;
    }

    public int[] neighbours(int exam) {
        return neighbours[exam];
    }

    public int degree(int exam) {
        return neighbours[exam].length;
    }

    public int edgeCount() {
        return weights.size();
    }

    public int weight(int a, int b) {
        return weights.getOrDefault(key(Math.min(a, b), Math.max(a, b), size), 0);
    }

    @Override
    public String toString() {
        return "ConflictGraph" + Arrays.deepToString(neighbours);
    }
}
