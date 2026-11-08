package com.slotforge.auth.controller;

import com.slotforge.auth.dto.CreateUserRequest;
import com.slotforge.auth.dto.UpdateRolesRequest;
import com.slotforge.auth.dto.UpdateUserRequest;
import com.slotforge.auth.dto.UserResponse;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.entity.UserStatus;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.auth.service.UserService;
import com.slotforge.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService users;

    @GetMapping("/api/users")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
        return users.search(q, role, status, pageable);
    }

    @GetMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse get(@PathVariable Long id) {
        return users.get(id);
    }

    @PostMapping("/api/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@AuthenticationPrincipal AppPrincipal actor, @Valid @RequestBody CreateUserRequest body) {
        return users.create(actor.id(), body);
    }

    @PatchMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse rename(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody UpdateUserRequest body) {
        return users.rename(actor.id(), id, body.fullName());
    }

    @PutMapping("/api/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeRoles(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody UpdateRolesRequest body) {
        return users.changeRoles(actor.id(), id, body.roles());
    }

    @PostMapping("/api/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse deactivate(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        return users.deactivate(actor.id(), id);
    }

    @PostMapping("/api/users/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse activate(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        return users.activate(actor.id(), id);
    }
}
