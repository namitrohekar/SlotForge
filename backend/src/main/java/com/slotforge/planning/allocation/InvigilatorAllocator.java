package com.slotforge.planning.allocation;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slotforge.planning.model.InvigilatorInfo;

/** Staffs halls greedily, giving each duty to whoever has the fewest so far. A shortage is left for the validator to report. */
public final class InvigilatorAllocator {

    /** One hall of one paper. key identifies it in the result. */
    public record Duty(long key, long slotId, int needed) {
    }

    private InvigilatorAllocator() {
    }

    /** One person per started block of studentsPerInvigilator seats. */
    public static int needed(int seats, int studentsPerInvigilator) {
        return (seats + studentsPerInvigilator - 1) / studentsPerInvigilator;
    }

    /** Duties are filled in the order given. A list shorter than the duty needs means nobody else was eligible. */
    public static Map<Long, List<Long>> allocate(List<Duty> duties, List<InvigilatorInfo> staff, int maxDuties) {
        Map<Long, Integer> load = new HashMap<>();
        Map<Long, Set<Long>> busyInSlot = new HashMap<>();
        Map<Long, List<Long>> assigned = new LinkedHashMap<>();
        for (Duty duty : duties) {
            Set<Long> busy = busyInSlot.computeIfAbsent(duty.slotId(), ignored -> new HashSet<>());
            List<Long> chosen = staff.stream()
                    .filter(person -> !person.unavailableSlots().contains(duty.slotId()))
                    .filter(person -> !busy.contains(person.id()))
                    .filter(person -> load.getOrDefault(person.id(), 0) < maxDuties)
                    .sorted(Comparator.<InvigilatorInfo>comparingInt(person -> load.getOrDefault(person.id(), 0))
                            .thenComparingLong(InvigilatorInfo::id))
                    .limit(duty.needed())
                    .map(InvigilatorInfo::id)
                    .toList();
            chosen.forEach(id -> {
                load.merge(id, 1, Integer::sum);
                busy.add(id);
            });
            assigned.put(duty.key(), chosen);
        }
        return assigned;
    }
}
