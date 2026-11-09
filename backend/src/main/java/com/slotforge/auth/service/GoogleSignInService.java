package com.slotforge.auth.service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import com.slotforge.audit.service.AuditService;
import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.common.config.SlotForgeProperties;
import com.slotforge.common.exception.ApiException;
import com.slotforge.student.entity.Student;
import com.slotforge.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a Google identity Spring has already verified into a SlotForge account.
 * Roles only ever come from the database, or the fixed STUDENT role for someone on the roster.
 */
@Service
@Profile("google")
@RequiredArgsConstructor
public class GoogleSignInService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSignInService.class);

    private final AppUserRepository users;
    private final StudentRepository students;
    private final AuditService audit;
    private final SlotForgeProperties properties;

    /** @throws ApiException whose code says why this person cannot sign in */
    @Transactional
    public AppUser signIn(OidcUser identity) {
        requireAllowed(identity);
        String sub = identity.getSubject();
        String email = identity.getEmail().toLowerCase(Locale.ROOT);

        AppUser user = users.findByGoogleSub(sub)
                .map(known -> withCurrentEmail(known, email))
                .or(() -> users.findByEmail(email).map(invited -> bound(invited, sub)))
                .orElseGet(() -> fromRoster(email, sub));
        if (!user.isActive()) throw refused("ACCOUNT_DEACTIVATED");

        user.setLastLoginAt(Instant.now());
        audit.record(user.getId(), "LOGIN", "app_user", user.getId(), "{\"method\":\"google\"}");
        return user;
    }

    // Read from the signed token. A hd parameter on the request could be altered by the browser.
    private void requireAllowed(OidcUser identity) {
        String domain = identity.getClaimAsString("hd");
        Set<String> allowed = properties.google().allowedDomains();
        if (domain == null || allowed.stream().noneMatch(parent -> isWithin(domain, parent))) {
            log.info("Google sign-in refused: hosted domain {} is not allowed", domain);
            throw refused("DOMAIN_NOT_ALLOWED");
        }
        if (!Boolean.TRUE.equals(identity.getEmailVerified())) throw refused("EMAIL_NOT_VERIFIED");
    }

    // Departments have their own subdomains. The dot stops notvjti.ac.in matching vjti.ac.in.
    private static boolean isWithin(String domain, String parent) {
        String host = domain.toLowerCase(Locale.ROOT);
        String allowed = parent.toLowerCase(Locale.ROOT);
        return host.equals(allowed) || host.endsWith("." + allowed);
    }

    private AppUser withCurrentEmail(AppUser user, String email) {
        if (!email.equals(user.getEmail())) {
            if (users.findByEmail(email).isPresent()) throw refused("ACCOUNT_CONFLICT");
            user.setEmail(email);
        }
        return user;
    }

    // A different sub on the same address means the address was reassigned to someone else.
    private AppUser bound(AppUser user, String sub) {
        if (user.getGoogleSub() != null) throw refused("ACCOUNT_CONFLICT");
        user.setGoogleSub(sub);
        return user;
    }

    private AppUser fromRoster(String email, String sub) {
        Student student = students.findUnlinkedActiveByEmail(email).orElseThrow(() -> refused("NOT_REGISTERED"));
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setFullName(student.getFullName());
        user.setGoogleSub(sub);
        user.setRoles(EnumSet.of(Role.STUDENT));
        users.save(user);
        students.linkAccount(student.getId(), user.getId());
        return user;
    }

    private static ApiException refused(String code) {
        return new ApiException(HttpStatus.FORBIDDEN, code, "Google sign-in refused: " + code);
    }
}
