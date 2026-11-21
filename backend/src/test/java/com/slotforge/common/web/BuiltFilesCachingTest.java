package com.slotforge.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.auth.security.SecurityConfig;
import com.slotforge.common.health.HealthController;
import com.slotforge.common.health.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, BuiltFilesCaching.class})
@TestPropertySource(properties = "spring.web.resources.static-locations=classpath:/built-app/")
class BuiltFilesCachingTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AppUserRepository users;

    @MockitoBean
    private HealthService health;

    @Test
    void hashedBuildFilesAreKeptForAYear() throws Exception {
        mvc.perform(get("/assets/index-abc123.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"));
    }

    @Test
    void theAppPageIsNeverCachedSoADeployShowsAtOnce() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
        mvc.perform(get("/sessions/13"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }
}
