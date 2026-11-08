package com.slotforge.student.repository;

import java.util.Collection;
import java.util.List;

import com.slotforge.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    @Override
    @EntityGraph(attributePaths = {"batch", "batch.curriculum", "batch.curriculum.course"})
    Page<Student> findAll(Specification<Student> spec, Pageable pageable);

    boolean existsByRollNoIgnoreCaseAndIdNot(String rollNo, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    /** Rows of [studentId, number of papers enrolled in]. */
    @Query(value = "SELECT student_id, count(*) FROM examination_enrolment WHERE student_id IN (:ids) GROUP BY student_id",
            nativeQuery = true)
    List<Object[]> enrolmentCounts(@Param("ids") Collection<Long> studentIds);
}
