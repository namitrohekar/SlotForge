package com.slotforge.academic.controller;

import com.slotforge.academic.dto.SubjectRequest;
import com.slotforge.academic.dto.SubjectResponse;
import com.slotforge.academic.service.SubjectService;
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
public class SubjectController {

    private final SubjectService service;

    @GetMapping("/api/subjects")
    @PreAuthorize(Access.STAFF)
    public PageResponse<SubjectResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long departmentId,
            @PageableDefault(size = 20, sort = "code") Pageable pageable) {
        return service.search(q, departmentId, pageable);
    }

    @GetMapping("/api/subjects/{id}")
    @PreAuthorize(Access.STAFF)
    public SubjectResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/api/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.COORDINATOR)
    public SubjectResponse create(@AuthenticationPrincipal AppPrincipal actor, @Valid @RequestBody SubjectRequest body) {
        return service.create(actor.id(), body);
    }

    @PutMapping("/api/subjects/{id}")
    @PreAuthorize(Access.COORDINATOR)
    public SubjectResponse update(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody SubjectRequest body) {
        return service.update(actor.id(), id, body);
    }

    @DeleteMapping("/api/subjects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.COORDINATOR)
    public void delete(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        service.delete(actor.id(), id);
    }
}
