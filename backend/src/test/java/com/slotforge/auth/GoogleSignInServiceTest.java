package com.slotforge.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import com.slotforge.audit.service.AuditService;
import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.entity.UserStatus;
import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.auth.service.GoogleSignInService;
import com.slotforge.common.config.SlotForgeProperties;
import com.slotforge.common.exception.ApiException;
import com.slotforge.student.entity.Student;
import com.slotforge.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class GoogleSignInServiceTest {

    private static final String EMAIL = "r@mc.vjti.ac.in";

    private final AppUserRepository users = mock(AppUserRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final GoogleSignInService service = serviceAllowing("mc.vjti.ac.in");

    private GoogleSignInService serviceAllowing(String... domains) {
        return new GoogleSignInService(users, students, audit,
                new SlotForgeProperties("origin", null, new SlotForgeProperties.Google(Set.of(domains))));
    }

    private static OidcUser identity(String email, boolean verified, String domain) {
        Instant now = Instant.now();
        OidcIdToken.Builder token = OidcIdToken.withTokenValue("t").subject("sub-1").issuedAt(now).expiresAt(now.plusSeconds(60))
                .claim("email", email).claim("email_verified", verified);
        if (domain != null) token.claim("hd", domain);
        return new DefaultOidcUser(AuthorityUtils.createAuthorityList("OIDC_USER"), token.build());
    }

    private static OidcUser valid() {
        return identity(EMAIL, true, "mc.vjti.ac.in");
    }

    private static AppUser account(String sub) {
        AppUser user = new AppUser();
        user.setId(5L);
        user.setEmail(EMAIL);
        user.setGoogleSub(sub);
        user.setRoles(EnumSet.of(Role.ADMIN));
        return user;
    }

    private void assertRefused(GoogleSignInService target, OidcUser identity, String code) {
        assertThatThrownBy(() -> target.signIn(identity))
                .isInstanceOfSatisfying(ApiException.class, refused -> assertThat(refused.getCode()).isEqualTo(code));
    }

    @Test
    void onlyTheAllowedHostedDomainsCanSignIn() {
        assertRefused(service, identity("x@gmail.com", true, null), "DOMAIN_NOT_ALLOWED");
        assertRefused(service, identity("x@other.ac.in", true, "other.ac.in"), "DOMAIN_NOT_ALLOWED");
        assertRefused(serviceAllowing(), valid(), "DOMAIN_NOT_ALLOWED");
        verify(users, never()).findByEmail(any());
    }

    @Test
    void theDomainComparisonIgnoresCase() {
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.of(account("sub-1")));

        assertThat(service.signIn(identity(EMAIL, true, "MC.VJTI.AC.IN")).getId()).isEqualTo(5L);
    }

    @Test
    void aParentDomainCoversDepartmentsButNotLookalikes() {
        GoogleSignInService vjti = serviceAllowing("vjti.ac.in");
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.of(account("sub-1")));

        assertThat(vjti.signIn(valid()).getId()).isEqualTo(5L);
        assertRefused(vjti, identity(EMAIL, true, "notvjti.ac.in"), "DOMAIN_NOT_ALLOWED");
        assertRefused(vjti, identity(EMAIL, true, "vjti.ac.in.evil.com"), "DOMAIN_NOT_ALLOWED");
    }

    @Test
    void anUnverifiedEmailIsRefused() {
        assertRefused(service, identity(EMAIL, false, "mc.vjti.ac.in"), "EMAIL_NOT_VERIFIED");
    }

    @Test
    void aReturningUserIsFoundByGoogleIdAndKeepsTheirRoles() {
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.of(account("sub-1")));

        AppUser user = service.signIn(valid());

        assertThat(user.getRoles()).containsExactly(Role.ADMIN);
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(audit).record(5L, "LOGIN", "app_user", 5L, "{\"method\":\"google\"}");
    }

    @Test
    void aChangedGoogleEmailIsFollowedUnlessSomeoneElseHasIt() {
        AppUser renamed = account("sub-1");
        renamed.setEmail("old@mc.vjti.ac.in");
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.of(renamed));

        assertThat(service.signIn(valid()).getEmail()).isEqualTo(EMAIL);

        renamed.setEmail("old@mc.vjti.ac.in");
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(account("sub-2")));
        assertRefused(service, valid(), "ACCOUNT_CONFLICT");
    }

    @Test
    void anAccountCreatedByAnAdministratorBindsOnFirstSignIn() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(account(null)));

        assertThat(service.signIn(valid()).getGoogleSub()).isEqualTo("sub-1");
    }

    @Test
    void aReassignedAddressCannotTakeOverTheOldAccount() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(account("someone-else")));

        assertRefused(service, valid(), "ACCOUNT_CONFLICT");
    }

    @Test
    void aStudentOnTheRosterGetsAnAccountWithOnlyTheStudentRole() {
        Student student = new Student();
        student.setId(10L);
        student.setFullName("Roster Name");
        when(students.findUnlinkedActiveByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(users.save(any())).thenAnswer(call -> {
            call.<AppUser>getArgument(0).setId(9L);
            return call.getArgument(0);
        });

        AppUser user = service.signIn(valid());

        assertThat(user.getRoles()).containsExactly(Role.STUDENT);
        assertThat(user.getFullName()).isEqualTo("Roster Name");
        verify(students).linkAccount(10L, 9L);
    }

    @Test
    void someoneUnknownIsNotRegistered() {
        assertRefused(service, valid(), "NOT_REGISTERED");
        verify(users, never()).save(any());
    }

    @Test
    void aDeactivatedAccountIsRefused() {
        AppUser user = account("sub-1");
        user.setStatus(UserStatus.DEACTIVATED);
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.of(user));

        assertRefused(service, valid(), "ACCOUNT_DEACTIVATED");
    }
}
