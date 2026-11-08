package com.slotforge.auth.security;

import java.io.Serializable;

import com.slotforge.auth.entity.AppUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Who is signed in. Every sign-in method ends in this same principal, so the rest of the app has one shape to trust. */
public record AppPrincipal(Long id, String email, String fullName) implements Serializable {

    public static Authentication authenticationFor(AppUser user) {
        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
        var principal = new AppPrincipal(user.getId(), user.getEmail(), user.getFullName());
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
    }
}
