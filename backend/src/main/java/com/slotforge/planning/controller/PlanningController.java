package com.slotforge.planning.controller;

import com.slotforge.auth.security.Access;
import com.slotforge.auth.security.AppPrincipal;
import java.util.List;

import com.slotforge.planning.comparison.AlgorithmComparison;
import com.slotforge.planning.dto.BaselineResponse;
import com.slotforge.planning.dto.RunResponse;
import com.slotforge.planning.preflight.FeasibilityAnalyzer;
import com.slotforge.planning.service.PlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PlanningController {

    private final PlanningService planning;

    @PostMapping("/api/exam-sessions/{id}/planning/runs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.COORDINATOR)
    public RunResponse generate(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        return planning.generate(actor.id(), id);
    }

    @GetMapping("/api/exam-sessions/{id}/planning/preflight")
    @PreAuthorize(Access.STUDENT_RECORDS)
    public List<FeasibilityAnalyzer.Finding> preflight(@PathVariable Long id) {
        return planning.preflight(id);
    }

    @GetMapping("/api/exam-sessions/{id}/planning/baseline")
    @PreAuthorize(Access.STUDENT_RECORDS)
    public BaselineResponse baseline(@PathVariable Long id) {
        return planning.baseline(id);
    }

    @GetMapping("/api/planning/comparison")
    @PreAuthorize(Access.STAFF)
    public AlgorithmComparison.Report comparison() {
        return planning.compareAlgorithms();
    }
}
