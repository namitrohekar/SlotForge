package com.slotforge.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumSet;
import java.util.List;

import com.slotforge.auth.controller.AuthController;
import com.slotforge.auth.controller.DemoLoginController;
import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.auth.security.SecurityConfig;
import com.slotforge.auth.service.DemoLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest({DemoLoginController.class, AuthController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class DemoLoginControllerTest {

    private static final String BODY = "{\"userId\":1}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DemoLoginService demoLogin;

    @MockitoBean
    private AppUserRepository users;

    @Test
    void optionsOfferDemoLoginUnderTheLocalProfile() throws Exception {
        mvc.perform(get("/api/auth/options")).andExpect(jsonPath("$.demoLoginEnabled").value(true));
    }

    @Test
    void listsDemoUsers() throws Exception {
        when(demoLogin.demoUsers()).thenReturn(List.of(admin()));

        mvc.perform(get("/api/auth/demo-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@demo.slotforge.test"))
                .andExpect(jsonPath("$[0].roles[0]").value("ADMIN"));
    }

    @Test
    void signInStoresTheUserInANewSession() throws Exception {
        when(demoLogin.signIn(1L)).thenReturn(admin());
        MockHttpSession before = new MockHttpSession();
        String oldId = before.getId();

        MvcResult result = mvc.perform(post("/api/auth/demo-login").session(before).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session.getId()).isNotEqualTo(oldId);
        assertThat(session.getAttribute("SPRING_SECURITY_CONTEXT")).isNotNull();
    }

    @Test
    void signInFromAnotherMachineIsRefused() throws Exception {
        mvc.perform(post("/api/auth/demo-login").with(csrf())
                        .with(request -> { request.setRemoteAddr("192.168.1.20"); return request; })
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DEMO_LOGIN_LOCAL_ONLY"));
        verify(demoLogin, never()).signIn(any());
    }

    @Test
    void missingUserIdIsAValidationFailure() throws Exception {
        mvc.perform(post("/api/auth/demo-login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("userId"));
    }

    static AppUser admin() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("admin@demo.slotforge.test");
        user.setFullName("Demo Admin");
        user.setRoles(EnumSet.of(Role.ADMIN));
        return user;
    }
}
