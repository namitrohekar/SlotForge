package com.slotforge.exam.repository;

import java.util.Collection;
import java.util.List;

import com.slotforge.exam.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findBySessionIdOrderBySlotDateAscStartTimeAsc(Long sessionId);

    /** Rows of [slotId, number of timetable entries using it]. */
    @Query(value = "SELECT time_slot_id, count(*) FROM exam_schedule WHERE time_slot_id IN (:ids) GROUP BY time_slot_id", nativeQuery = true)
    List<Object[]> usageCounts(@Param("ids") Collection<Long> slotIds);
}
