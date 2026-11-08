package com.slotforge.auth.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

/** Security failures happen before any controller runs, so they write the same problem JSON directly. */
final class SecurityErrors {

    private SecurityErrors() {
    }

    static void write(HttpServletResponse response, HttpStatus status, String code, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"code\":\"%s\",\"detail\":\"%s\"}"
                        .formatted(status.getReasonPhrase(), status.value(), code, detail));
    }
}
