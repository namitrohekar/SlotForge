package com.slotforge.common.health;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final HealthService health;

    @GetMapping("/api/health")
    @PreAuthorize("hasRole('ADMIN')")
    public HealthResponse health() {
        return health.read();
    }
}
