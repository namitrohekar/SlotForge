package com.slotforge.academic.repository;

import java.util.Collection;
import java.util.List;

import com.slotforge.academic.entity.Curriculum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long>, JpaSpecificationExecutor<Curriculum> {

    @Override
    @EntityGraph(attributePaths = "course")
    Page<Curriculum> findAll(Specification<Curriculum> spec, Pageable pageable);

    boolean existsByCourseIdAndNameIgnoreCaseAndIdNot(Long courseId, String name, Long id);

    /** Rows of [courseId, number of curricula]. */
    @Query("select c.course.id, count(c) from Curriculum c where c.course.id in :ids group by c.course.id")
    List<Object[]> curriculumCounts(@Param("ids") Collection<Long> courseIds);

    /** Rows of [curriculumId, subjects placed, batches following it]. */
    @Query(value = """
            SELECT c.id,
                   (SELECT count(*) FROM curriculum_subject cs WHERE cs.curriculum_id = c.id),
                   (SELECT count(*) FROM batch b WHERE b.curriculum_id = c.id)
            FROM curriculum c WHERE c.id IN (:ids)""", nativeQuery = true)
    List<Object[]> usage(@Param("ids") Collection<Long> curriculumIds);
}
