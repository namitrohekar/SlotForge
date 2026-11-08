package com.slotforge.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.slotforge.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Demo sign-in against real PostgreSQL: session, roles, audit row and deactivation. */
@ActiveProfiles("local")
@AutoConfigureMockMvc
class AuthFlowTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void adminSignsInSeesSystemStatusAndIsAudited() throws Exception {
        long adminId = createUser("admin-" + UUID.randomUUID() + "@demo.slotforge.test", "ADMIN");

        mvc.perform(get("/api/auth/demo-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].roles[0]".formatted(adminId)).value("ADMIN"));

        MockHttpSession session = signIn(adminId);

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adminId))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
        mvc.perform(get("/api/health").session(session)).andExpect(status().isOk());

        assertThat(jdbc.sql("SELECT count(*) FROM audit_log WHERE actor_user_id = ? AND action = 'LOGIN'")
                .param(adminId).query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT last_login_at IS NOT NULL FROM app_user WHERE id = ?")
                .param(adminId).query(Boolean.class).single()).isTrue();
    }

    @Test
    void deactivationEndsTheSessionOnTheNextRequest() throws Exception {
        long userId = createUser("coord-" + UUID.randomUUID() + "@demo.slotforge.test", "EXAM_COORDINATOR");
        MockHttpSession session = signIn(userId);

        jdbc.sql("UPDATE app_user SET status = 'DEACTIVATED' WHERE id = ?").param(userId).update();

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DEACTIVATED"));
    }

    @Test
    void studentCannotSeeSystemStatus() throws Exception {
        MockHttpSession session = signIn(createUser("stu-" + UUID.randomUUID() + "@demo.slotforge.test", "STUDENT"));

        mvc.perform(get("/api/health").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void realAccountsCannotUseDemoSignIn() throws Exception {
        long realId = createUser("real-" + UUID.randomUUID() + "@vjti.ac.in", "ADMIN");

        mvc.perform(post("/api/auth/demo-login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + realId + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_A_DEMO_USER"));
    }

    private MockHttpSession signIn(long userId) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/demo-login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + userId + "}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private long createUser(String email, String role) {
        long id = jdbc.sql("INSERT INTO app_user (email, full_name) VALUES (?, 'Test user') RETURNING id")
                .param(email).query(Long.class).single();
        jdbc.sql("INSERT INTO user_role (user_id, role) VALUES (?, ?)").param(id).param(role).update();
        return id;
    }
}
