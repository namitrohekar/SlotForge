package com.slotforge.auth;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumSet;
import java.util.Optional;

import com.slotforge.auth.controller.AuthController;
import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.entity.UserStatus;
import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.auth.security.SecurityConfig;
import com.slotforge.common.health.HealthController;
import com.slotforge.common.health.HealthResponse;
import com.slotforge.common.health.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AuthController.class, HealthController.class})
@Import(SecurityConfig.class)
class SecurityRulesTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AppUserRepository users;

    @MockitoBean
    private HealthService health;

    @Test
    void signedOutApiCallIsJson401() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void optionsArePublicAndIssueTheCsrfCookie() throws Exception {
        mvc.perform(get("/api/auth/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleEnabled").value(false))
                .andExpect(jsonPath("$.demoLoginEnabled").value(false))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    @Test
    void demoEndpointsDoNotExistWithoutTheLocalProfile() throws Exception {
        mvc.perform(get("/api/auth/demo-users")).andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/demo-login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/auth/logout").with(authentication(signedIn(user(1L, Role.ADMIN)))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        AppUser admin = user(1L, Role.ADMIN);
        when(users.findById(1L)).thenReturn(Optional.of(admin));

        mvc.perform(post("/api/auth/logout").with(csrf()).with(authentication(signedIn(admin))))
                .andExpect(status().isNoContent());
    }

    @Test
    void meReturnsTheSignedInUser() throws Exception {
        AppUser hod = user(4L, Role.HOD);
        when(users.findById(4L)).thenReturn(Optional.of(hod));

        mvc.perform(get("/api/auth/me").with(authentication(signedIn(hod))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.email").value("user4@demo.slotforge.test"))
                .andExpect(jsonPath("$.roles[0]").value("HOD"));
    }

    @Test
    void deactivatedUserIsSignedOutOnTheNextRequest() throws Exception {
        AppUser admin = user(1L, Role.ADMIN);
        Authentication session = signedIn(admin);
        admin.setStatus(UserStatus.DEACTIVATED);
        when(users.findById(1L)).thenReturn(Optional.of(admin));

        mvc.perform(get("/api/auth/me").with(authentication(session)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DEACTIVATED"));
    }

    @Test
    void deletedUserIsSignedOutOnTheNextRequest() throws Exception {
        when(users.findById(9L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/auth/me").with(authentication(signedIn(user(9L, Role.ADMIN)))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DEACTIVATED"));
    }

    @Test
    void roleChangesApplyOnTheNextRequest() throws Exception {
        AppUser person = user(2L, Role.FACULTY);
        Authentication session = signedIn(person);
        person.setRoles(EnumSet.of(Role.ADMIN));
        when(users.findById(2L)).thenReturn(Optional.of(person));
        when(health.read()).thenReturn(new HealthResponse("UP", "slotforge", "18.6", 2, 2));

        mvc.perform(get("/api/health").with(authentication(session))).andExpect(status().isOk());
    }

    @Test
    void wrongRoleIsJson403() throws Exception {
        AppUser student = user(6L, Role.STUDENT);
        when(users.findById(6L)).thenReturn(Optional.of(student));

        mvc.perform(get("/api/health").with(authentication(signedIn(student))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void nonApiPathsAreLeftToTheFrontend() throws Exception {
        // Not an API route, so security lets it through and the app answers 404 as JSON.
        mvc.perform(get("/students")).andExpect(status().isNotFound());
    }

    private static Authentication signedIn(AppUser user) {
        return AppPrincipal.authenticationFor(user);
    }

    private static AppUser user(Long id, Role... roles) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail("user" + id + "@demo.slotforge.test");
        user.setFullName("User " + id);
        user.setRoles(EnumSet.of(roles[0], roles));
        return user;
    }
}
