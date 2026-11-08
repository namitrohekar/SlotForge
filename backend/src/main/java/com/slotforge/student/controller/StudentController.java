package com.slotforge.student.controller;

import com.slotforge.student.dto.StudentRequest;
import com.slotforge.student.dto.StudentResponse;
import com.slotforge.student.service.StudentService;
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
public class StudentController {

    private final StudentService service;

    @GetMapping("/api/students")
    @PreAuthorize(Access.STUDENT_RECORDS)
    public PageResponse<StudentResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long batchId,
            @PageableDefault(size = 20, sort = "rollNo") Pageable pageable) {
        return service.search(q, batchId, pageable);
    }

    @GetMapping("/api/students/{id}")
    @PreAuthorize(Access.STUDENT_RECORDS)
    public StudentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/api/students")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.COORDINATOR)
    public StudentResponse create(@AuthenticationPrincipal AppPrincipal actor, @Valid @RequestBody StudentRequest body) {
        return service.create(actor.id(), body);
    }

    @PutMapping("/api/students/{id}")
    @PreAuthorize(Access.COORDINATOR)
    public StudentResponse update(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody StudentRequest body) {
        return service.update(actor.id(), id, body);
    }

    @DeleteMapping("/api/students/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.COORDINATOR)
    public void delete(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        service.delete(actor.id(), id);
    }
}
