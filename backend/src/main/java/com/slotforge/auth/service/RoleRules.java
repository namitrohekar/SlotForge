package com.slotforge.auth.service;

import java.util.Set;

import com.slotforge.auth.entity.Role;
import com.slotforge.common.exception.ApiException;

/**
 * Which roles one account may hold together. The frontend mirrors these in utils/roles.js.
 * Only staff combinations the model supports are allowed: one administrative role plus one staff role.
 */
public final class RoleRules {

    private static final Set<Role> STAFF = Set.of(Role.FACULTY, Role.HOD, Role.CONTROLLER, Role.INVIGILATOR);
    private static final Set<Role> ADMINISTRATION = Set.of(Role.ADMIN, Role.EXAM_COORDINATOR);

    private RoleRules() {
    }

    /** Null when the set is allowed, otherwise the reason in plain words. */
    public static String problem(Set<Role> roles) {
        if (roles.contains(Role.STUDENT) && roles.size() > 1) {
            return "A student account cannot also hold a staff or administrative role.";
        }
        if (count(roles, STAFF) > 1) {
            return "Choose only one of Faculty, Head of department, Controller of Examinations or Invigilator. "
                    + "A head of department already has faculty access, and invigilator is for non-teaching staff.";
        }
        if (count(roles, ADMINISTRATION) > 1) {
            return "Choose Administrator or Exam coordinator, not both. An administrator already has coordinator access.";
        }
        if ((roles.contains(Role.HOD) || roles.contains(Role.CONTROLLER)) && count(roles, ADMINISTRATION) > 0) {
            return "Whoever approves timetables cannot also be an administrator or exam coordinator, "
                    + "who generate and publish them.";
        }
        return null;
    }

    public static void requireValid(Set<Role> roles) {
        String problem = problem(roles);
        if (problem != null) throw ApiException.unprocessable("INVALID_ROLE_COMBINATION", problem);
    }

    private static long count(Set<Role> roles, Set<Role> group) {
        return roles.stream().filter(group::contains).count();
    }
}
