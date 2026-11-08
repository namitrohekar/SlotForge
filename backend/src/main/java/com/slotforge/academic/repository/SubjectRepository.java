package com.slotforge.academic.repository;

import java.util.Collection;
import java.util.List;

import com.slotforge.academic.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubjectRepository extends JpaRepository<Subject, Long>, JpaSpecificationExecutor<Subject> {

    @Override
    @EntityGraph(attributePaths = "department")
    Page<Subject> findAll(Specification<Subject> spec, Pageable pageable);

    @EntityGraph(attributePaths = "department")
    List<Subject> findByIdIn(Collection<Long> ids);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    /** Rows of [departmentId, number of subjects]. */
    @Query("select s.department.id, count(s) from Subject s where s.department.id in :ids group by s.department.id")
    List<Object[]> subjectCounts(@Param("ids") Collection<Long> departmentIds);

    /** Rows of [subjectId, number of curricula that place it]. */
    @Query(value = "SELECT subject_id, count(*) FROM curriculum_subject WHERE subject_id IN (:ids) GROUP BY subject_id",
            nativeQuery = true)
    List<Object[]> curriculumCounts(@Param("ids") Collection<Long> subjectIds);
}
