package com.slotforge.academic.controller;

import com.slotforge.academic.dto.BatchRequest;
import com.slotforge.academic.dto.BatchResponse;
import com.slotforge.academic.service.BatchService;
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
public class BatchController {

    private final BatchService service;

    @GetMapping("/api/batches")
    @PreAuthorize(Access.STAFF)
    public PageResponse<BatchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long courseId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.search(q, courseId, pageable);
    }

    @GetMapping("/api/batches/{id}")
    @PreAuthorize(Access.STAFF)
    public BatchResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/api/batches")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.COORDINATOR)
    public BatchResponse create(@AuthenticationPrincipal AppPrincipal actor, @Valid @RequestBody BatchRequest body) {
        return service.create(actor.id(), body);
    }

    @PutMapping("/api/batches/{id}")
    @PreAuthorize(Access.COORDINATOR)
    public BatchResponse update(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody BatchRequest body) {
        return service.update(actor.id(), id, body);
    }

    @DeleteMapping("/api/batches/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.COORDINATOR)
    public void delete(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        service.delete(actor.id(), id);
    }
}
