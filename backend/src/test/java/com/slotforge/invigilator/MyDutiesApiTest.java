package com.slotforge.invigilator;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.auth.security.SecurityConfig;
import com.slotforge.invigilator.controller.MyDutiesController;
import com.slotforge.invigilator.service.MyDutiesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MyDutiesController.class)
@Import(SecurityConfig.class)
class MyDutiesApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MyDutiesService service;

    @MockitoBean
    private AppUserRepository users;

    @Test
    void invigilatorsSeeOnlyTheirOwnDuties() throws Exception {
        when(service.duties(5L)).thenReturn(List.of());

        mvc.perform(get("/api/me/duties").with(authentication(signedIn(5L, Role.INVIGILATOR)))).andExpect(status().isOk());
    }

    @Test
    void studentsHaveNoDuties() throws Exception {
        mvc.perform(get("/api/me/duties").with(authentication(signedIn(6L, Role.STUDENT)))).andExpect(status().isForbidden());
    }

    @Test
    void unavailableDatesNeedDatesAndAReason() throws Exception {
        mvc.perform(post("/api/me/unavailability").with(csrf()).with(authentication(signedIn(5L, Role.INVIGILATOR)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
    }

    private Authentication signedIn(Long id, Role role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail("user" + id + "@vjti.ac.in");
        user.setFullName("User " + id);
        user.setRoles(EnumSet.of(role));
        when(users.findById(id)).thenReturn(Optional.of(user));
        return AppPrincipal.authenticationFor(user);
    }
}
