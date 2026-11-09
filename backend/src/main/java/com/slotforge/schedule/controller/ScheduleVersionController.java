package com.slotforge.schedule.controller;

import java.util.List;

import com.slotforge.auth.security.Access;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.schedule.dto.AwaitingApprovalResponse;
import com.slotforge.schedule.dto.DutyAtRiskResponse;
import com.slotforge.schedule.dto.ReturnRequest;
import com.slotforge.schedule.dto.ScheduleEntryResponse;
import com.slotforge.schedule.dto.VersionResponse;
import com.slotforge.schedule.service.ScheduleQueryService;
import com.slotforge.schedule.service.ScheduleWorkflowService;
import com.slotforge.schedule.validation.ScheduleValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Drafts are visible only to the people who produce and review them. */
@RestController
@RequiredArgsConstructor
public class ScheduleVersionController {

    private final ScheduleQueryService schedules;
    private final ScheduleWorkflowService workflow;
    private final ScheduleValidator validator;

    @GetMapping("/api/exam-sessions/{id}/schedule-versions")
    @PreAuthorize(Access.STUDENT_RECORDS)
    public List<VersionResponse> versions(@PathVariable Long id) {
        return schedules.versions(id);
    }

    @GetMapping("/api/schedule-versions/awaiting-approval")
    @PreAuthorize(Access.HOD)
    public List<AwaitingApprovalResponse> awaitingApproval(@AuthenticationPrincipal AppPrincipal actor) {
        return schedules.awaitingApproval(actor.id());
    }

    @GetMapping("/api/schedule-versions/duties-at-risk")
    @PreAuthorize(Access.COORDINATOR)
    public List<DutyAtRiskResponse> dutiesAtRisk() {
        return schedules.dutiesAtRisk();
    }

    @GetMapping("/api/schedule-versions/{id}/entries")
    @PreAuthorize(Access.STUDENT_RECORDS)
    public List<ScheduleEntryResponse> entries(@PathVariable Long id) {
        return schedules.entries(id);
    }

    @GetMapping("/api/schedule-versions/{id}/violations")
    @PreAuthorize(Access.STUDENT_RECORDS)
    public List<String> violations(@PathVariable Long id) {
        return validator.problems(id);
    }

    @PostMapping("/api/schedule-versions/{id}/submit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.COORDINATOR)
    public void submit(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        workflow.submit(actor.id(), id);
    }

    @PostMapping("/api/schedule-versions/{id}/review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.HOD)
    public void approve(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        workflow.approve(actor.id(), id);
    }

    @PostMapping("/api/schedule-versions/{id}/return")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.HOD)
    public void sendBack(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody ReturnRequest body) {
        workflow.sendBack(actor.id(), id, body.comment());
    }

    @PostMapping("/api/schedule-versions/{id}/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.COORDINATOR)
    public void publish(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        workflow.publish(actor.id(), id);
    }

    @DeleteMapping("/api/schedule-versions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.COORDINATOR)
    public void deleteDraft(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        schedules.deleteDraft(actor.id(), id);
    }
}
