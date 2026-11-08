package com.slotforge.auth.dto;

import java.util.List;

import com.slotforge.auth.security.AppPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public record CurrentUserResponse(Long id, String email, String fullName, List<String> roles) {

    public static CurrentUserResponse from(Authentication authentication) {
        AppPrincipal principal = (AppPrincipal) authentication.getPrincipal();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.substring("ROLE_".length()))
                .sorted()
                .toList();
        return new CurrentUserResponse(principal.id(), principal.email(), principal.fullName(), roles);
    }
}
