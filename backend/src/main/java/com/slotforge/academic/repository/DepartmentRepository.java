package com.slotforge.academic.repository;

import com.slotforge.academic.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {

    @Override
    @EntityGraph(attributePaths = "head")
    Page<Department> findAll(Specification<Department> spec, Pageable pageable);

    @Modifying
    @Query(value = "UPDATE department SET head_user_id = NULL WHERE head_user_id = :userId", nativeQuery = true)
    int vacateHead(@Param("userId") Long userId);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
