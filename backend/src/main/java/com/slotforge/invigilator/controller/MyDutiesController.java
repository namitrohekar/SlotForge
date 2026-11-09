package com.slotforge.invigilator.controller;

import java.util.List;

import com.slotforge.auth.security.Access;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.invigilator.dto.DutyResponse;
import com.slotforge.invigilator.dto.UnavailabilityRequest;
import com.slotforge.invigilator.dto.UnavailabilityResponse;
import com.slotforge.invigilator.service.MyDutiesService;
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

@RestController
@RequiredArgsConstructor
public class MyDutiesController {

    private final MyDutiesService service;

    @GetMapping("/api/me/duties")
    @PreAuthorize(Access.INVIGILATORS)
    public List<DutyResponse> duties(@AuthenticationPrincipal AppPrincipal actor) {
        return service.duties(actor.id());
    }

    @GetMapping("/api/me/unavailability")
    @PreAuthorize(Access.INVIGILATORS)
    public List<UnavailabilityResponse> unavailability(@AuthenticationPrincipal AppPrincipal actor) {
        return service.unavailability(actor.id());
    }

    @PostMapping("/api/me/unavailability")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.INVIGILATORS)
    public UnavailabilityResponse add(@AuthenticationPrincipal AppPrincipal actor, @Valid @RequestBody UnavailabilityRequest body) {
        return service.add(actor.id(), body);
    }

    @DeleteMapping("/api/me/unavailability/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.INVIGILATORS)
    public void remove(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        service.remove(actor.id(), id);
    }
}
