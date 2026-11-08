package com.slotforge.exam.repository;

import java.util.Collection;
import java.util.List;

import com.slotforge.exam.entity.Examination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExaminationRepository extends JpaRepository<Examination, Long>, JpaSpecificationExecutor<Examination> {

    @Override
    @EntityGraph(attributePaths = {"subject", "subject.department"})
    Page<Examination> findAll(Specification<Examination> spec, Pageable pageable);

    List<Examination> findBySessionId(Long sessionId);

    /** Rows of [examinationId, number of students]. */
    @Query(value = "SELECT examination_id, count(*) FROM examination_enrolment WHERE examination_id IN (:ids) GROUP BY examination_id",
            nativeQuery = true)
    List<Object[]> enrolmentCounts(@Param("ids") Collection<Long> examinationIds);

    @Query(value = "SELECT count(*) FROM examination_enrolment WHERE examination_id = :id", nativeQuery = true)
    long enrolled(@Param("id") Long examinationId);
}
