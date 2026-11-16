package com.slotforge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.slotforge.common.response.PageResponse;
import com.slotforge.hall.controller.HallController;
import com.slotforge.hall.service.HallService;
import com.slotforge.student.controller.StudentController;
import com.slotforge.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({HallController.class, StudentController.class})
@Import(SecurityConfig.class)
class RosterApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private HallService halls;

    @MockitoBean
    private com.slotforge.hall.service.HallOutageService outages;

    @MockitoBean
    private StudentService students;

    @MockitoBean
    private AppUserRepository users;

    @Test
    void invigilatorsCanSeeHalls() throws Exception {
        when(halls.search(eq(null), eq(null), any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/halls").with(authentication(signedIn(5L, Role.INVIGILATOR)))).andExpect(status().isOk());
    }

    @Test
    void invigilatorsCannotSeeTheStudentRoster() throws Exception {
        mvc.perform(get("/api/students").with(authentication(signedIn(5L, Role.INVIGILATOR))))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentsCannotReadOtherStudents() throws Exception {
        mvc.perform(get("/api/students").with(authentication(signedIn(6L, Role.STUDENT))))
                .andExpect(status().isForbidden());
    }

    @Test
    void facultyCannotChangeHalls() throws Exception {
        mvc.perform(post("/api/halls").with(csrf()).with(authentication(signedIn(4L, Role.FACULTY)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"A1\",\"name\":\"A\",\"building\":\"B\",\"capacity\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void hallCapacityAndCodeAreValidated() throws Exception {
        mvc.perform(post("/api/halls").with(csrf()).with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"A 1!\",\"name\":\"A\",\"building\":\"B\",\"capacity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void negativeFloorsAndAbsurdCapacitiesAreRefused() throws Exception {
        mvc.perform(post("/api/halls").with(csrf()).with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"A1\",\"name\":\"A\",\"building\":\"B\",\"floor\":-1,\"capacity\":5000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void studentFieldsAreValidated() throws Exception {
        mvc.perform(post("/api/students").with(csrf()).with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rollNo\":\"\",\"fullName\":\"A\",\"email\":\"nope\"}"))
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
