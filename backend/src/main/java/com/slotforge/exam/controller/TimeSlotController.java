package com.slotforge.exam.controller;

import com.slotforge.auth.security.Access;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.exam.dto.GenerateSlotsRequest;
import com.slotforge.exam.dto.GenerateSlotsResponse;
import com.slotforge.exam.dto.SlotStatusRequest;
import com.slotforge.exam.dto.TimeSlotResponse;
import com.slotforge.exam.service.TimeSlotService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TimeSlotController {

    private final TimeSlotService service;

    @GetMapping("/api/exam-sessions/{id}/time-slots")
    @PreAuthorize(Access.STAFF)
    public List<TimeSlotResponse> list(@PathVariable Long id) {
        return service.list(id);
    }

    @PostMapping("/api/exam-sessions/{id}/time-slots/generate")
    @PreAuthorize(Access.COORDINATOR)
    public GenerateSlotsResponse generate(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody GenerateSlotsRequest body) {
        return service.generate(actor.id(), id, body);
    }

    @PatchMapping("/api/time-slots/{id}/status")
    @PreAuthorize(Access.COORDINATOR)
    public TimeSlotResponse changeStatus(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody SlotStatusRequest body) {
        return service.changeStatus(actor.id(), id, body);
    }

    @DeleteMapping("/api/time-slots/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.COORDINATOR)
    public void delete(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        service.delete(actor.id(), id);
    }
}
