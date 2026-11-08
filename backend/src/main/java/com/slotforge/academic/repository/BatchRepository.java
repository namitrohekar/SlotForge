package com.slotforge.academic.repository;

import java.util.Collection;
import java.util.List;

import com.slotforge.academic.entity.Batch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatchRepository extends JpaRepository<Batch, Long>, JpaSpecificationExecutor<Batch> {

    @Override
    @EntityGraph(attributePaths = {"curriculum", "curriculum.course"})
    Page<Batch> findAll(Specification<Batch> spec, Pageable pageable);

    boolean existsByCurriculumIdAndNameIgnoreCaseAndIdNot(Long curriculumId, String name, Long id);

    /** Rows of [batchId, number of students]. */
    @Query(value = "SELECT batch_id, count(*) FROM student WHERE batch_id IN (:ids) GROUP BY batch_id", nativeQuery = true)
    List<Object[]> studentCounts(@Param("ids") Collection<Long> batchIds);
}
