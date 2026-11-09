package com.slotforge.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.Set;

import com.slotforge.auth.entity.Role;
import com.slotforge.auth.service.RoleRules;
import com.slotforge.common.exception.ApiException;
import org.junit.jupiter.api.Test;

class RoleRulesTest {

    @Test
    void everySingleRoleIsAllowed() {
        for (Role role : Role.values()) {
            assertThat(RoleRules.problem(Set.of(role))).as(role.name()).isNull();
        }
    }

    @Test
    void aStudentHoldsNoOtherRole() {
        for (Role other : EnumSet.complementOf(EnumSet.of(Role.STUDENT))) {
            assertThat(RoleRules.problem(Set.of(Role.STUDENT, other))).as(other.name()).contains("student");
        }
    }

    @Test
    void oneStaffRoleAtMost() {
        assertThat(RoleRules.problem(Set.of(Role.HOD, Role.FACULTY))).isNotNull();
        assertThat(RoleRules.problem(Set.of(Role.FACULTY, Role.INVIGILATOR))).isNotNull();
        assertThat(RoleRules.problem(Set.of(Role.HOD, Role.INVIGILATOR))).isNotNull();
    }

    @Test
    void administratorAlreadyIncludesCoordinator() {
        assertThat(RoleRules.problem(Set.of(Role.ADMIN, Role.EXAM_COORDINATOR))).isNotNull();
    }

    @Test
    void theReviewerCannotAlsoPublish() {
        assertThat(RoleRules.problem(Set.of(Role.HOD, Role.ADMIN))).isNotNull();
        assertThat(RoleRules.problem(Set.of(Role.HOD, Role.EXAM_COORDINATOR))).isNotNull();
        assertThat(RoleRules.problem(Set.of(Role.CONTROLLER, Role.ADMIN))).isNotNull();
        assertThat(RoleRules.problem(Set.of(Role.CONTROLLER, Role.EXAM_COORDINATOR))).isNotNull();
        assertThat(RoleRules.problem(Set.of(Role.CONTROLLER, Role.HOD))).isNotNull();
    }

    @Test
    void anAdministrativeRoleMayJoinTeachingOrNonTeachingStaff() {
        assertThat(RoleRules.problem(Set.of(Role.EXAM_COORDINATOR, Role.FACULTY))).isNull();
        assertThat(RoleRules.problem(Set.of(Role.ADMIN, Role.FACULTY))).isNull();
        assertThat(RoleRules.problem(Set.of(Role.EXAM_COORDINATOR, Role.INVIGILATOR))).isNull();
    }

    @Test
    void allRolesTogetherAreRejectedWithAClearCode() {
        assertThatThrownBy(() -> RoleRules.requireValid(EnumSet.allOf(Role.class)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("INVALID_ROLE_COMBINATION");
    }
}
