package com.slotforge.exam.repository;

import java.util.Collection;
import java.util.List;

import com.slotforge.exam.entity.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamSessionRepository extends JpaRepository<ExamSession, Long>, JpaSpecificationExecutor<ExamSession> {

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Rows of [sessionId, number of slots]. */
    @Query(value = "SELECT session_id, count(*) FROM time_slot WHERE session_id IN (:ids) GROUP BY session_id", nativeQuery = true)
    List<Object[]> slotCounts(@Param("ids") Collection<Long> sessionIds);

    /** Rows of [sessionId, number of papers]. */
    @Query(value = "SELECT session_id, count(*) FROM examination WHERE session_id IN (:ids) GROUP BY session_id", nativeQuery = true)
    List<Object[]> examinationCounts(@Param("ids") Collection<Long> sessionIds);
}
