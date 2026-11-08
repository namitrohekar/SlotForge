package com.slotforge.academic.controller;

import com.slotforge.academic.dto.CurriculumRequest;
import com.slotforge.academic.dto.CurriculumResponse;
import com.slotforge.academic.dto.CurriculumSubjectsRequest;
import com.slotforge.academic.dto.CurriculumSubjectResponse;
import com.slotforge.academic.service.CurriculumService;
import com.slotforge.auth.security.Access;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.common.response.PageResponse;
import java.util.List;

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
public class CurriculumController {

    private final CurriculumService service;

    @GetMapping("/api/curricula")
    @PreAuthorize(Access.STAFF)
    public PageResponse<CurriculumResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long courseId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.search(q, courseId, pageable);
    }

    @GetMapping("/api/curricula/{id}")
    @PreAuthorize(Access.STAFF)
    public CurriculumResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/api/curricula")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.COORDINATOR)
    public CurriculumResponse create(@AuthenticationPrincipal AppPrincipal actor, @Valid @RequestBody CurriculumRequest body) {
        return service.create(actor.id(), body);
    }

    @PutMapping("/api/curricula/{id}")
    @PreAuthorize(Access.COORDINATOR)
    public CurriculumResponse update(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody CurriculumRequest body) {
        return service.update(actor.id(), id, body);
    }

    @DeleteMapping("/api/curricula/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.COORDINATOR)
    public void delete(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        service.delete(actor.id(), id);
    }

    @GetMapping("/api/curricula/{id}/subjects")
    @PreAuthorize(Access.STAFF)
    public List<CurriculumSubjectResponse> subjects(@PathVariable Long id) {
        return service.subjects(id);
    }

    @PutMapping("/api/curricula/{id}/subjects")
    @PreAuthorize(Access.COORDINATOR)
    public List<CurriculumSubjectResponse> replaceSubjects(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id,
            @Valid @RequestBody CurriculumSubjectsRequest body) {
        return service.replaceSubjects(actor.id(), id, body.subjects());
    }
}
