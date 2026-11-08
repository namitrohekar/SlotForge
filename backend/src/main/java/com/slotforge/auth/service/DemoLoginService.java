package com.slotforge.auth.service;

import java.time.Instant;
import java.util.List;

import com.slotforge.audit.service.AuditService;
import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.UserStatus;
import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("local & !prod")
@RequiredArgsConstructor
public class DemoLoginService {

    // .test is a reserved domain, so a real account can never match.
    static final String DEMO_DOMAIN = "@demo.slotforge.test";

    private final AppUserRepository users;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<AppUser> demoUsers() {
        return users.findByEmailEndingWithAndStatusOrderByIdAsc(DEMO_DOMAIN, UserStatus.ACTIVE);
    }

    @Transactional
    public AppUser signIn(Long userId) {
        AppUser user = users.findById(userId)
                .filter(candidate -> candidate.isActive() && candidate.getEmail().endsWith(DEMO_DOMAIN))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "NOT_A_DEMO_USER",
                        "Only active demo accounts can use demo sign-in."));
        user.setLastLoginAt(Instant.now());
        audit.record(user.getId(), "LOGIN", "app_user", user.getId(), "{\"method\":\"demo\"}");
        return user;
    }
}
