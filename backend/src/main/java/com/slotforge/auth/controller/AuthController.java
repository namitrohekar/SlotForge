package com.slotforge.auth.controller;

import com.slotforge.auth.dto.AuthOptionsResponse;
import com.slotforge.auth.dto.CurrentUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final Environment environment;

    // Public, and how the browser gets its CSRF cookie: the token is lazy, so reading it writes the cookie.
    @GetMapping("/api/auth/options")
    @PreAuthorize("permitAll()")
    public AuthOptionsResponse options(CsrfToken csrfToken) {
        csrfToken.getToken();
        return new AuthOptionsResponse(false, environment.matchesProfiles("local & !prod"));
    }

    @GetMapping("/api/auth/me")
    @PreAuthorize("isAuthenticated()")
    public CurrentUserResponse me(Authentication authentication, CsrfToken csrfToken) {
        csrfToken.getToken();
        return CurrentUserResponse.from(authentication);
    }
}
