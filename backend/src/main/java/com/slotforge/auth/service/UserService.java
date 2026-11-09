package com.slotforge.auth.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.slotforge.academic.service.DepartmentService;
import com.slotforge.audit.service.AuditService;
import com.slotforge.auth.dto.CreateUserRequest;
import com.slotforge.auth.dto.UserResponse;
import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.entity.UserStatus;
import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.common.exception.ApiException;
import com.slotforge.common.persistence.QuerySupport;
import com.slotforge.common.response.PageResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> SORTABLE = Set.of("fullName", "email", "createdAt", "lastLoginAt");
    // Any fixed number shared by every change that can remove an administrator.
    private static final long ADMIN_CHANGE_LOCK = 4_001_001L;

    private final AppUserRepository users;
    private final AuditService audit;
    private final JdbcClient jdbc;
    private final DepartmentService departments;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String q, Role role, UserStatus status, Pageable pageable) {
        QuerySupport.requireSortable(pageable, SORTABLE);
        Specification<AppUser> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String pattern = QuerySupport.containsPattern(q);
                where.add(cb.or(
                        cb.like(cb.lower(root.get("email")), pattern, '\\'),
                        cb.like(cb.lower(root.get("fullName")), pattern, '\\')));
            }
            if (role != null) where.add(cb.equal(root.join("roles"), role));
            if (status != null) where.add(cb.equal(root.get("status"), status));
            return cb.and(where.toArray(Predicate[]::new));
        };
        return PageResponse.from(users.findAll(spec, pageable).map(UserResponse::from));
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(find(id));
    }

    @Transactional
    public UserResponse create(Long actorId, CreateUserRequest request) {
        RoleRules.requireValid(request.roles());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("DUPLICATE", "A user with this email already exists.");
        }
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setRoles(EnumSet.copyOf(request.roles()));
        users.save(user);
        audit.record(actorId, "USER_CREATED", "app_user", user.getId(), rolesJson(user.getRoles()));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse rename(Long actorId, Long id, String fullName) {
        AppUser user = findEditable(id);
        user.setFullName(fullName.trim());
        audit.record(actorId, "USER_UPDATED", "app_user", id, null);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse changeRoles(Long actorId, Long id, Set<Role> roles) {
        refuseSelf(actorId, id, "You cannot change your own roles.");
        AppUser user = findEditable(id);
        RoleRules.requireValid(roles);
        if (user.isActive() && user.getRoles().contains(Role.ADMIN) && !roles.contains(Role.ADMIN)) {
            requireAnotherAdmin(id);
        }
        if (user.getRoles().contains(Role.HOD) && !roles.contains(Role.HOD)) departments.vacateHead(actorId, id);
        user.getRoles().clear();
        user.getRoles().addAll(roles);
        audit.record(actorId, "USER_ROLES_CHANGED", "app_user", id, rolesJson(roles));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse deactivate(Long actorId, Long id) {
        refuseSelf(actorId, id, "You cannot deactivate your own account.");
        AppUser user = find(id);
        if (!user.isActive()) return UserResponse.from(user);
        if (user.getRoles().contains(Role.ADMIN)) requireAnotherAdmin(id);
        if (user.getRoles().contains(Role.HOD)) departments.vacateHead(actorId, id);
        user.setStatus(UserStatus.DEACTIVATED);
        audit.record(actorId, "USER_DEACTIVATED", "app_user", id, null);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse activate(Long actorId, Long id) {
        AppUser user = find(id);
        if (user.isActive()) return UserResponse.from(user);
        // Roles can only break the rules if changed outside the API. Such an account stays off until fixed.
        RoleRules.requireValid(user.getRoles());
        user.setStatus(UserStatus.ACTIVE);
        audit.record(actorId, "USER_ACTIVATED", "app_user", id, null);
        return UserResponse.from(user);
    }

    private AppUser find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User", id));
    }

    // A deactivated account is frozen until an administrator explicitly reactivates it.
    private AppUser findEditable(Long id) {
        AppUser user = find(id);
        if (!user.isActive()) {
            throw ApiException.conflict("USER_DEACTIVATED", "Reactivate this account before editing it.");
        }
        return user;
    }

    private static void refuseSelf(Long actorId, Long id, String message) {
        if (id.equals(actorId)) throw ApiException.unprocessable("SELF_ROLE_CHANGE", message);
    }

    // Two admins demoting each other at once must not leave nobody in charge. The lock makes those
    // changes take turns, and the count runs after it, so it sees the other change once committed.
    private void requireAnotherAdmin(Long id) {
        jdbc.sql("SELECT pg_advisory_xact_lock(?)").param(ADMIN_CHANGE_LOCK).query((rs, row) -> true).list();
        if (users.countWithRoleExcept(Role.ADMIN, UserStatus.ACTIVE, id) == 0) {
            throw ApiException.unprocessable("LAST_ADMIN", "SlotForge needs at least one active administrator.");
        }
    }

    // Enum names only, so this is always valid JSON.
    private static String rolesJson(Collection<Role> roles) {
        return roles.stream().map(Enum::name).sorted().map(name -> "\"" + name + "\"")
                .collect(Collectors.joining(",", "{\"roles\":[", "]}"));
    }
}
