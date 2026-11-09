package com.slotforge.planning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slotforge.planning.allocation.InvigilatorAllocator;
import com.slotforge.planning.allocation.InvigilatorAllocator.Duty;
import com.slotforge.planning.model.InvigilatorInfo;
import org.junit.jupiter.api.Test;

class InvigilatorAllocatorTest {

    private static InvigilatorInfo person(long id, Long... closedSlots) {
        return new InvigilatorInfo(id, Set.of(closedSlots));
    }

    @Test
    void oneInvigilatorPerStartedBlockOfStudents() {
        assertThat(InvigilatorAllocator.needed(1, 30)).isEqualTo(1);
        assertThat(InvigilatorAllocator.needed(30, 30)).isEqualTo(1);
        assertThat(InvigilatorAllocator.needed(31, 30)).isEqualTo(2);
        assertThat(InvigilatorAllocator.needed(75, 30)).isEqualTo(3);
    }

    @Test
    void nobodyIsInTwoHallsInOneSlot() {
        Map<Long, List<Long>> result = InvigilatorAllocator.allocate(
                List.of(new Duty(1, 10, 1), new Duty(2, 10, 1)), List.of(person(1), person(2)), 8);

        assertThat(result.get(1L)).containsExactly(1L);
        assertThat(result.get(2L)).containsExactly(2L);
    }

    @Test
    void dutiesGoToWhoeverHasFewest() {
        Map<Long, List<Long>> result = InvigilatorAllocator.allocate(
                List.of(new Duty(1, 10, 1), new Duty(2, 11, 1), new Duty(3, 12, 1), new Duty(4, 13, 1)), List.of(person(1), person(2)), 8);

        assertThat(result.values()).extracting(list -> list.get(0)).containsExactly(1L, 2L, 1L, 2L);
    }

    @Test
    void someoneUnavailableInThatSlotIsSkipped() {
        Map<Long, List<Long>> result = InvigilatorAllocator.allocate(List.of(new Duty(1, 10, 1)), List.of(person(1, 10L), person(2)), 8);

        assertThat(result.get(1L)).containsExactly(2L);
    }

    @Test
    void theDutyLimitHolds() {
        Map<Long, List<Long>> result = InvigilatorAllocator.allocate(List.of(new Duty(1, 10, 1), new Duty(2, 11, 1)), List.of(person(1)), 1);

        assertThat(result.get(1L)).containsExactly(1L);
        assertThat(result.get(2L)).isEmpty();
    }

    @Test
    void aShortageLeavesTheDutyPartlyFilled() {
        Map<Long, List<Long>> result = InvigilatorAllocator.allocate(List.of(new Duty(1, 10, 3)), List.of(person(1), person(2)), 8);

        assertThat(result.get(1L)).containsExactly(1L, 2L);
    }
}
