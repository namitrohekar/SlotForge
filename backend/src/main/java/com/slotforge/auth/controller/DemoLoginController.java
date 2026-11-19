package com.slotforge.auth.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import com.slotforge.auth.dto.CurrentUserResponse;
import com.slotforge.auth.dto.DemoLoginRequest;
import com.slotforge.auth.dto.DemoUserResponse;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.auth.service.DemoLoginService;
import com.slotforge.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Exists only under DemoLoginService.PROFILES. Without them these URLs are 404. */
@RestController
@Profile(DemoLoginService.PROFILES)
@RequiredArgsConstructor
public class DemoLoginController {

    private final DemoLoginService demoLogin;
    private final Environment environment;
    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    @GetMapping("/api/auth/demo-users")
    @PreAuthorize("permitAll()")
    public List<DemoUserResponse> demoUsers() {
        return demoLogin.demoUsers().stream().map(DemoUserResponse::from).toList();
    }

    @PostMapping("/api/auth/demo-login")
    @PreAuthorize("permitAll()")
    public CurrentUserResponse signIn(
            @Valid @RequestBody DemoLoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        // On a developer machine the demo stays on that machine. A demo deployment is public on purpose.
        if (!environment.matchesProfiles("demo") && !isLoopback(request.getRemoteAddr())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "DEMO_LOGIN_LOCAL_ONLY", "Demo sign-in only works on this computer.");
        }

        Authentication authentication = AppPrincipal.authenticationFor(demoLogin.signIn(body.userId()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        request.getSession(true);
        request.changeSessionId();
        contexts.saveContext(context, request, response);
        return CurrentUserResponse.from(authentication);
    }

    private static boolean isLoopback(String address) {
        try {
            return InetAddress.getByName(address).isLoopbackAddress();
        } catch (UnknownHostException ex) {
            return false;
        }
    }
}
