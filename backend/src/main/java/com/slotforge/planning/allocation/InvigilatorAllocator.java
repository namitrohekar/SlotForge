package com.slotforge.planning.allocation;

import java.util.ArrayList;
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

    public static Map<Long, List<Long>> allocate(List<Duty> duties, List<InvigilatorInfo> staff, int maxDuties) {
        return allocate(duties, staff, maxDuties, Map.of());
    }

    /**
     * Duties are filled in the order given. A list shorter than the duty needs means nobody else was eligible.
     * keep names the people a duty already had. Each stays while still eligible, before anyone new is chosen for any duty.
     */
    public static Map<Long, List<Long>> allocate(List<Duty> duties, List<InvigilatorInfo> staff, int maxDuties, Map<Long, List<Long>> keep) {
        Map<Long, InvigilatorInfo> pool = new HashMap<>();
        staff.forEach(person -> pool.put(person.id(), person));
        Map<Long, Integer> load = new HashMap<>();
        Map<Long, Set<Long>> busyInSlot = new HashMap<>();
        Map<Long, List<Long>> assigned = new LinkedHashMap<>();
        for (Duty duty : duties) {
            Set<Long> busy = busyInSlot.computeIfAbsent(duty.slotId(), ignored -> new HashSet<>());
            List<Long> staying = new ArrayList<>();
            for (Long id : keep.getOrDefault(duty.key(), List.of())) {
                InvigilatorInfo person = pool.get(id);
                boolean eligible = person != null && !person.unavailableSlots().contains(duty.slotId()) && !busy.contains(id)
                        && load.getOrDefault(id, 0) < maxDuties && staying.size() < duty.needed();
                if (!eligible) continue;
                staying.add(id);
                load.merge(id, 1, Integer::sum);
                busy.add(id);
            }
            assigned.put(duty.key(), staying);
        }
        for (Duty duty : duties) {
            Set<Long> busy = busyInSlot.get(duty.slotId());
            List<Long> chosen = assigned.get(duty.key());
            List<Long> added = staff.stream()
                    .filter(person -> !person.unavailableSlots().contains(duty.slotId()))
                    .filter(person -> !busy.contains(person.id()))
                    .filter(person -> load.getOrDefault(person.id(), 0) < maxDuties)
                    .sorted(Comparator.<InvigilatorInfo>comparingInt(person -> load.getOrDefault(person.id(), 0))
                            .thenComparingLong(InvigilatorInfo::id))
                    .limit(Math.max(0, duty.needed() - chosen.size()))
                    .map(InvigilatorInfo::id)
                    .toList();
            added.forEach(id -> {
                load.merge(id, 1, Integer::sum);
                busy.add(id);
            });
            chosen.addAll(added);
        }
        return assigned;
    }
}
