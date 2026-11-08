package com.slotforge.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import com.slotforge.planning.graph.ConflictGraph;
import com.slotforge.planning.graph.ConflictGraphBuilder;
import com.slotforge.planning.model.ExamNode;
import org.junit.jupiter.api.Test;

class ConflictGraphBuilderTest {

    private static ExamNode exam(long id, Long... students) {
        return new ExamNode(id, "E" + id, Set.of(students), false, 60, null, null);
    }

    @Test
    void anEmptySessionHasNoEdges() {
        assertThat(ConflictGraphBuilder.build(List.of()).edgeCount()).isZero();
    }

    @Test
    void examsWithoutSharedStudentsAreNotConnected() {
        ConflictGraph graph = ConflictGraphBuilder.build(List.of(exam(1, 10L), exam(2, 20L)));

        assertThat(graph.edgeCount()).isZero();
        assertThat(graph.neighbours(0)).isEmpty();
    }

    @Test
    void theWeightIsTheNumberOfSharedStudents() {
        ConflictGraph graph = ConflictGraphBuilder.build(List.of(exam(1, 10L, 11L, 12L), exam(2, 10L, 11L), exam(3, 12L)));

        assertThat(graph.edgeCount()).isEqualTo(2);
        assertThat(graph.weight(0, 1)).isEqualTo(2);
        assertThat(graph.weight(1, 0)).isEqualTo(2);
        assertThat(graph.weight(0, 2)).isEqualTo(1);
        assertThat(graph.weight(1, 2)).isZero();
        assertThat(graph.neighbours(0)).containsExactly(1, 2);
        assertThat(graph.degree(2)).isEqualTo(1);
    }

    @Test
    void aStudentWithThreeExamsConnectsEveryPairOnce() {
        ConflictGraph graph = ConflictGraphBuilder.build(List.of(exam(1, 10L), exam(2, 10L), exam(3, 10L)));

        assertThat(graph.edgeCount()).isEqualTo(3);
    }

    @Test
    void unsortedOrRepeatedIdsAreRefused() {
        assertThatThrownBy(() -> ConflictGraphBuilder.build(List.of(exam(2, 10L), exam(1, 10L))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConflictGraphBuilder.build(List.of(exam(1, 10L), exam(1, 11L))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
