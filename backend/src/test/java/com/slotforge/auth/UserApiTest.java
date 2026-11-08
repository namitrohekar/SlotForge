package com.slotforge.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import com.slotforge.auth.controller.UserController;
import com.slotforge.auth.dto.UserResponse;
import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.entity.UserStatus;
import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.auth.security.SecurityConfig;
import com.slotforge.auth.service.UserService;
import com.slotforge.common.response.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserService service;

    @MockitoBean
    private AppUserRepository users;

    @Test
    void onlyAdministratorsManageUsers() throws Exception {
        mvc.perform(get("/api/users").with(authentication(signedIn(5L, Role.EXAM_COORDINATOR))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listsUsersAsAPage() throws Exception {
        UserResponse row = new UserResponse(2L, "hod@vjti.ac.in", "A Head", List.of("HOD"), UserStatus.ACTIVE, null, null);
        when(service.search(eq("hod"), eq(Role.HOD), eq(null), any())).thenReturn(new PageResponse<>(List.of(row), 0, 20, 1, 1));

        mvc.perform(get("/api/users?q=hod&role=HOD").with(authentication(signedIn(1L, Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("hod@vjti.ac.in"))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void unknownRoleFilterIsAValidationFailure() throws Exception {
        mvc.perform(get("/api/users?role=KING").with(authentication(signedIn(1L, Role.ADMIN))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createRejectsMissingFieldsAndEmptyRoles() throws Exception {
        mvc.perform(post("/api/users").with(csrf()).with(authentication(signedIn(1L, Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"fullName\":\"\",\"roles\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
    }

    @Test
    void createIsRecordedAgainstTheSignedInAdministrator() throws Exception {
        UserResponse created = new UserResponse(9L, "new@vjti.ac.in", "New", List.of("HOD"), UserStatus.ACTIVE, null, null);
        when(service.create(eq(1L), any())).thenReturn(created);

        mvc.perform(post("/api/users").with(csrf()).with(authentication(signedIn(1L, Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@vjti.ac.in\",\"fullName\":\"New\",\"roles\":[\"HOD\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
        verify(service).create(eq(1L), any());
    }

    @Test
    void rolesMustBeKnown() throws Exception {
        mvc.perform(put("/api/users/2/roles").with(csrf()).with(authentication(signedIn(1L, Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"KING\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("roles[0]"));
    }

    // Signs in as a real AppPrincipal, which the active-user filter reloads from the mocked repository.
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
