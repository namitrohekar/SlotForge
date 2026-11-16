package com.slotforge.hall.controller;

import com.slotforge.hall.dto.HallOutageRequest;
import com.slotforge.hall.dto.HallOutageResponse;
import com.slotforge.hall.dto.HallRequest;
import com.slotforge.hall.dto.HallResponse;
import com.slotforge.hall.entity.HallStatus;
import com.slotforge.hall.service.HallOutageService;
import com.slotforge.hall.service.HallService;
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
public class HallController {

    private final HallService service;
    private final HallOutageService outages;

    @GetMapping("/api/halls")
    @PreAuthorize(Access.HALL_READERS)
    public PageResponse<HallResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) HallStatus status,
            @PageableDefault(size = 20, sort = "code") Pageable pageable) {
        return service.search(q, status, pageable);
    }

    @GetMapping("/api/halls/{id}")
    @PreAuthorize(Access.HALL_READERS)
    public HallResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/api/halls")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.COORDINATOR)
    public HallResponse create(@AuthenticationPrincipal AppPrincipal actor, @Valid @RequestBody HallRequest body) {
        return service.create(actor.id(), body);
    }

    @PutMapping("/api/halls/{id}")
    @PreAuthorize(Access.COORDINATOR)
    public HallResponse update(
            @AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody HallRequest body) {
        return service.update(actor.id(), id, body);
    }

    @DeleteMapping("/api/halls/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.COORDINATOR)
    public void delete(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id) {
        service.delete(actor.id(), id);
    }

    @GetMapping("/api/halls/{id}/unavailability")
    @PreAuthorize(Access.STUDENT_RECORDS)
    public java.util.List<HallOutageResponse> outages(@PathVariable Long id) {
        return outages.list(id);
    }

    @PostMapping("/api/halls/{id}/unavailability")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.COORDINATOR)
    public HallOutageResponse addOutage(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @Valid @RequestBody HallOutageRequest body) {
        return outages.add(actor.id(), id, body);
    }

    @DeleteMapping("/api/halls/{id}/unavailability/{outageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.COORDINATOR)
    public void removeOutage(@AuthenticationPrincipal AppPrincipal actor, @PathVariable Long id, @PathVariable Long outageId) {
        outages.remove(actor.id(), id, outageId);
    }
}
