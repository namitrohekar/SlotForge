package com.slotforge.exam.controller;

import com.slotforge.auth.security.Access;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.common.response.PageResponse;
import com.slotforge.exam.dto.EnrolledStudentResponse;
import com.slotforge.exam.dto.EnrolmentRequest;
import com.slotforge.exam.dto.EnrolmentResultResponse;
import com.slotforge.exam.service.EnrolmentService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EnrolmentController {

    private final EnrolmentService service;

    @GetMapping("/api/examinations/{id}/enrolments")
    @PreAuthorize(Access.STUDENT_RECORDS)
    public PageResponse<EnrolledStudentResponse> list(
            @PathVariable Long id, @RequestParam(required = false) String q, @PageableDefault(size = 20) Pageable pageable) {
        return service.list(id, q, pageable);
    }

    @PostMapping("/api/examinations/{id}/enrolments")
    @PreAuthorize(Access.COORDINATOR)
    public EnrolmentResultResponse add(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody EnrolmentRequest body) {
        return service.add(actor.id(), id, body.studentIds());
    }

    @DeleteMapping("/api/examinations/{id}/enrolments/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.COORDINATOR)
    public void remove(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @PathVariable Long studentId) {
        service.remove(actor.id(), id, studentId);
    }
}
