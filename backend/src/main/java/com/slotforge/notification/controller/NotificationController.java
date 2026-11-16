package com.slotforge.notification.controller;

import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.notification.dto.NotificationsResponse;
import com.slotforge.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifications;

    @GetMapping("/api/me/notifications")
    @PreAuthorize("isAuthenticated()")
    public NotificationsResponse list(@AuthenticationPrincipal AppPrincipal actor) {
        return notifications.list(actor.id());
    }

    @PostMapping("/api/me/notifications/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void markAllRead(@AuthenticationPrincipal AppPrincipal actor) {
        notifications.markAllRead(actor.id());
    }
}
