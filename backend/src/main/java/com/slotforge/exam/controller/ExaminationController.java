package com.slotforge.exam.controller;

import com.slotforge.auth.security.Access;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.common.response.PageResponse;
import com.slotforge.exam.dto.ExaminationResponse;
import com.slotforge.exam.dto.ExaminationUpdateRequest;
import com.slotforge.exam.dto.FromCurriculumRequest;
import com.slotforge.exam.dto.FromCurriculumResponse;
import com.slotforge.exam.service.ExaminationService;
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
public class ExaminationController {

    private final ExaminationService service;

    @GetMapping("/api/exam-sessions/{id}/examinations")
    @PreAuthorize(Access.STAFF)
    public PageResponse<ExaminationResponse> search(
            @PathVariable Long id, @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "subject.code") Pageable pageable) {
        return service.search(id, q, pageable);
    }

    @PostMapping("/api/exam-sessions/{id}/examinations/from-curriculum")
    @PreAuthorize(Access.COORDINATOR)
    public FromCurriculumResponse createFromCurriculum(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody FromCurriculumRequest body) {
        return service.createFromCurriculum(actor.id(), id, body);
    }

    @PutMapping("/api/examinations/{id}")
    @PreAuthorize(Access.COORDINATOR)
    public ExaminationResponse update(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody ExaminationUpdateRequest body) {
        return service.update(actor.id(), id, body);
    }

    @DeleteMapping("/api/examinations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.COORDINATOR)
    public void delete(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        service.delete(actor.id(), id);
    }
}
