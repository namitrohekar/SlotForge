package com.slotforge.common.health;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.auth.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class HealthApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private HealthService health;

    @MockitoBean
    private AppUserRepository users;

    @Test
    void reportsDatabaseAndSchemaVersion() throws Exception {
        when(health.read()).thenReturn(new HealthResponse("UP", "slotforge", "18.6", 1, 1));

        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("slotforge"))
                .andExpect(jsonPath("$.postgresVersion").value("18.6"))
                .andExpect(jsonPath("$.schemaVersion").value(1))
                .andExpect(jsonPath("$.requiredSchemaVersion").value(1));
    }

    @Test
    void databaseDownIsServiceUnavailable() throws Exception {
        when(health.read()).thenThrow(new CannotGetJdbcConnectionException("refused"));

        mvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DATABASE_UNAVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "FACULTY")
    void onlyAdminsSeeSystemStatus() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
