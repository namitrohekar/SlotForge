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
@ActiveProfiles({"prod", "demo"})
class PublicDemoLoginTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DemoLoginService demoLogin;

    @MockitoBean
    private AppUserRepository users;

    @Test
    void aDemoDeploymentOffersDemoSignInToAnyMachine() throws Exception {
        when(demoLogin.signIn(1L)).thenReturn(DemoLoginControllerTest.admin());

        mvc.perform(get("/api/auth/options")).andExpect(jsonPath("$.demoLoginEnabled").value(true));
        mvc.perform(post("/api/auth/demo-login").with(csrf())
                        .with(request -> { request.setRemoteAddr("203.0.113.7"); return request; })
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }
}
