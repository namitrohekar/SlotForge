package com.slotforge.hall.repository;

import java.util.Collection;
import java.util.List;

import com.slotforge.hall.entity.ExamHall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamHallRepository extends JpaRepository<ExamHall, Long>, JpaSpecificationExecutor<ExamHall> {

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    /** Rows of [hallId, number of allocations in any timetable]. */
    @Query(value = "SELECT hall_id, count(*) FROM hall_allocation WHERE hall_id IN (:ids) GROUP BY hall_id", nativeQuery = true)
    List<Object[]> allocationCounts(@Param("ids") Collection<Long> hallIds);
}
