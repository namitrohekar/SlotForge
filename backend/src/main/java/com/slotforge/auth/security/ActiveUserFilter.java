package com.slotforge.auth.security;

import java.io.IOException;

import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * One primary-key read per signed-in request. A deactivated user is signed out at once,
 * and role changes apply on the next request instead of waiting for a new session.
 */
public class ActiveUserFilter extends OncePerRequestFilter {

    private final AppUserRepository users;

    public ActiveUserFilter(AppUserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();

        if (current != null && current.getPrincipal() instanceof AppPrincipal principal) {
            AppUser user = users.findById(principal.id()).filter(AppUser::isActive).orElse(null);
            if (user == null) {
                HttpSession session = request.getSession(false);
                if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
                SecurityErrors.write(response, HttpStatus.UNAUTHORIZED, "ACCOUNT_DEACTIVATED",
                        "This account is no longer active.");
                return;
            }
            // Fresh roles for this request only. The session keeps what was saved at sign-in.
            SecurityContext fresh = SecurityContextHolder.createEmptyContext();
            fresh.setAuthentication(AppPrincipal.authenticationFor(user));
            SecurityContextHolder.setContext(fresh);
        }

        chain.doFilter(request, response);
    }
}
