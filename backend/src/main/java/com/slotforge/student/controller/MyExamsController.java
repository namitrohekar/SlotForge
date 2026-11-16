package com.slotforge.student.controller;

import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.student.dto.MyExamsResponse;
import com.slotforge.student.service.MyExamsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MyExamsController {

    private final MyExamsService service;

    @GetMapping("/api/me/exams")
    @PreAuthorize("hasRole('STUDENT')")
    public MyExamsResponse exams(@AuthenticationPrincipal AppPrincipal actor) {
        return service.exams(actor.id());
    }
}
