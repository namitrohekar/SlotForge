package com.slotforge.exam.controller;

import com.slotforge.auth.security.Access;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.common.response.PageResponse;
import com.slotforge.exam.dto.ExamSessionRequest;
import com.slotforge.exam.dto.ExamSessionResponse;
import com.slotforge.exam.dto.SessionStatusRequest;
import com.slotforge.exam.service.ExamSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class ExamSessionController {

    private final ExamSessionService service;

    @GetMapping("/api/exam-sessions")
    @PreAuthorize(Access.STAFF)
    public PageResponse<ExamSessionResponse> search(
            @RequestParam(required = false) String q, @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(q, pageable);
    }

    @GetMapping("/api/exam-sessions/{id}")
    @PreAuthorize(Access.STAFF)
    public ExamSessionResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/api/exam-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.COORDINATOR)
    public ExamSessionResponse create(@AuthenticationPrincipal AppPrincipal actor, @Valid @RequestBody ExamSessionRequest body) {
        return service.create(actor.id(), body);
    }

    @PutMapping("/api/exam-sessions/{id}")
    @PreAuthorize(Access.COORDINATOR)
    public ExamSessionResponse update(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody ExamSessionRequest body) {
        return service.update(actor.id(), id, body);
    }

    @PatchMapping("/api/exam-sessions/{id}/status")
    @PreAuthorize(Access.COORDINATOR)
    public ExamSessionResponse changeStatus(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody SessionStatusRequest body) {
        return service.changeStatus(actor.id(), id, body.status());
    }

    @DeleteMapping("/api/exam-sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.COORDINATOR)
    public void delete(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        service.delete(actor.id(), id);
    }
}
