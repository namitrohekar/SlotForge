package com.slotforge.auth.security;

import java.io.IOException;

import com.slotforge.auth.service.GoogleSignInService;
import com.slotforge.common.config.SlotForgeProperties;
import com.slotforge.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/** Replaces Google's token with the same principal demo sign-in uses, so the rest of the app sees one shape. */
@Component
@Profile("google")
@RequiredArgsConstructor
public class GoogleLoginHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleLoginHandler.class);

    private final GoogleSignInService signIn;
    private final SlotForgeProperties properties;
    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        try {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(AppPrincipal.authenticationFor(signIn.signIn((OidcUser) authentication.getPrincipal())));
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            response.sendRedirect(properties.frontendOrigin() + "/");
        } catch (RuntimeException failure) {
            if (!(failure instanceof ApiException)) log.error("Google sign-in failed", failure);
            refuse(request, response, failure instanceof ApiException refused ? refused.getCode() : "GOOGLE_ERROR");
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException failure)
            throws IOException {
        log.warn("Google sign-in did not complete: {}", failure.getMessage());
        refuse(request, response, "GOOGLE_ERROR");
    }

    private void refuse(HttpServletRequest request, HttpServletResponse response, String code) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        response.sendRedirect(properties.frontendOrigin() + "/login?error=" + code);
    }
}
