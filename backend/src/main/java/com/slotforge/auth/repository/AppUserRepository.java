package com.slotforge.auth.repository;

import java.util.List;
import java.util.Optional;

import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

    List<AppUser> findByEmailEndingWithAndStatusOrderByIdAsc(String suffix, UserStatus status);

    boolean existsByEmail(String email);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByGoogleSub(String googleSub);

    @Query("select count(u) from AppUser u join u.roles r where r = :role and u.status = :status and u.id <> :excludedId")
    long countWithRoleExcept(@Param("role") Role role, @Param("status") UserStatus status,
            @Param("excludedId") Long excludedId);
}
