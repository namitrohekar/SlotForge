package com.slotforge.academic.controller;

import com.slotforge.academic.dto.AppointHeadRequest;
import com.slotforge.academic.dto.DepartmentRequest;
import com.slotforge.academic.dto.DepartmentResponse;
import com.slotforge.academic.service.DepartmentService;
import com.slotforge.auth.security.Access;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService service;

    @GetMapping("/api/departments")
    @PreAuthorize(Access.STAFF)
    public PageResponse<DepartmentResponse> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "code") Pageable pageable) {
        return service.search(q, pageable);
    }

    @GetMapping("/api/departments/{id}")
    @PreAuthorize(Access.STAFF)
    public DepartmentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/api/departments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.COORDINATOR)
    public DepartmentResponse create(@AuthenticationPrincipal AppPrincipal actor, @Valid @RequestBody DepartmentRequest body) {
        return service.create(actor.id(), body);
    }

    @PutMapping("/api/departments/{id}")
    @PreAuthorize(Access.COORDINATOR)
    public DepartmentResponse update(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody DepartmentRequest body) {
        return service.update(actor.id(), id, body);
    }

    // Administrators only: a coordinator must not be able to pick who approves their timetable.
    @PutMapping("/api/departments/{id}/head")
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse appointHead(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @RequestBody AppointHeadRequest body) {
        return service.appointHead(actor.id(), id, body.userId());
    }

    @DeleteMapping("/api/departments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.COORDINATOR)
    public void delete(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        service.delete(actor.id(), id);
    }
}
