package com.slotforge.academic.repository;

import java.util.Collection;
import java.util.List;

import com.slotforge.academic.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    // The department is shown on every row, so it loads in the same query.
    @Override
    @EntityGraph(attributePaths = "department")
    Page<Course> findAll(Specification<Course> spec, Pageable pageable);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    /** Rows of [departmentId, number of courses]. */
    @Query("select c.department.id, count(c) from Course c where c.department.id in :ids group by c.department.id")
    List<Object[]> courseCounts(@Param("ids") Collection<Long> ids);

    /** The highest semester any curriculum or batch of this programme uses, or 0. */
    @Query(value = """
            SELECT GREATEST(
              COALESCE((SELECT max(cs.semester_no) FROM curriculum_subject cs
                        JOIN curriculum c ON c.id = cs.curriculum_id WHERE c.course_id = :id), 0),
              COALESCE((SELECT max(b.current_semester) FROM batch b
                        JOIN curriculum c ON c.id = b.curriculum_id WHERE c.course_id = :id), 0))""",
            nativeQuery = true)
    int highestSemesterInUse(@Param("id") Long courseId);
}
