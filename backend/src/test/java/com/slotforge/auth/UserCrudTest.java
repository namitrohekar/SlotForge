package com.slotforge.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.slotforge.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** User management through the API, with every result read back from PostgreSQL. */
@ActiveProfiles("local")
@AutoConfigureMockMvc
class UserCrudTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    private MockHttpSession admin;
    private long adminId;
    private String tag;

    @BeforeEach
    void signInAsAdmin() throws Exception {
        tag = UUID.randomUUID().toString().substring(0, 8);
        adminId = jdbc.sql("INSERT INTO app_user (email, full_name) VALUES (?, 'Test admin') RETURNING id")
                .param("admin-" + tag + "@demo.slotforge.test").query(Long.class).single();
        jdbc.sql("INSERT INTO user_role (user_id, role) VALUES (?, 'ADMIN')").param(adminId).update();
        admin = (MockHttpSession) mvc.perform(post("/api/auth/demo-login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + adminId + "}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    @Test
    void createsAUserWithRolesAndAnAuditRow() throws Exception {
        long id = create("New.Head-" + tag + "@VJTI.ac.in", "[\"EXAM_COORDINATOR\",\"FACULTY\"]");

        assertThat(jdbc.sql("SELECT email FROM app_user WHERE id = ?").param(id).query(String.class).single())
                .isEqualTo("new.head-" + tag + "@vjti.ac.in");
        assertThat(roles(id)).containsExactly("EXAM_COORDINATOR", "FACULTY");
        assertThat(jdbc.sql("SELECT count(*) FROM audit_log WHERE action = 'USER_CREATED' AND entity_id = ?")
                .param(String.valueOf(id)).query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void duplicateEmailIsRejectedAndNothingIsAdded() throws Exception {
        String email = "dup-" + tag + "@vjti.ac.in";
        create(email, "[\"STUDENT\"]");

        mvc.perform(post("/api/users").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(email.toUpperCase(), "[\"STUDENT\"]")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
        assertThat(jdbc.sql("SELECT count(*) FROM app_user WHERE email = ?").param(email).query(Long.class).single())
                .isEqualTo(1);
    }

    @Test
    void searchFindsByNameAndFiltersByRole() throws Exception {
        create("searchable-" + tag + "@vjti.ac.in", "[\"INVIGILATOR\"]");

        mvc.perform(get("/api/users").param("q", "SEARCHABLE-" + tag).param("role", "INVIGILATOR").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].roles[0]").value("INVIGILATOR"));
        mvc.perform(get("/api/users").param("q", "searchable-" + tag).param("role", "ADMIN").session(admin))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void invalidRoleCombinationsAreRejectedAndNothingIsStored() throws Exception {
        String studentAdmin = "student-admin-" + tag + "@vjti.ac.in";
        mvc.perform(post("/api/users").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(studentAdmin, "[\"STUDENT\",\"ADMIN\"]")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_ROLE_COMBINATION"));
        assertThat(jdbc.sql("SELECT count(*) FROM app_user WHERE email = ?").param(studentAdmin)
                .query(Long.class).single()).isZero();

        long hod = create("hod-" + tag + "@vjti.ac.in", "[\"HOD\"]");
        for (String roles : List.of("[\"HOD\",\"STUDENT\"]",
                "[\"ADMIN\",\"EXAM_COORDINATOR\",\"HOD\",\"FACULTY\",\"INVIGILATOR\",\"STUDENT\"]")) {
            mvc.perform(put("/api/users/" + hod + "/roles").session(admin).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":" + roles + "}"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("INVALID_ROLE_COMBINATION"));
        }
        assertThat(roles(hod)).containsExactly("HOD");
    }

    @Test
    void aDeactivatedStudentIsFrozenUntilReactivated() throws Exception {
        // A demo-domain email, so the student can use demo sign-in after reactivation.
        long student = create("student-" + tag + "@demo.slotforge.test", "[\"STUDENT\"]");
        mvc.perform(post("/api/users/" + student + "/deactivate").session(admin).with(csrf())).andExpect(status().isOk());

        mvc.perform(patch("/api/users/" + student).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Changed\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_DEACTIVATED"));
        mvc.perform(put("/api/users/" + student + "/roles").session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"FACULTY\"]}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.sql("SELECT full_name FROM app_user WHERE id = ?").param(student).query(String.class).single())
                .isEqualTo("Test person");

        mvc.perform(post("/api/users/" + student + "/activate").session(admin).with(csrf()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(patch("/api/users/" + student).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Changed\"}")).andExpect(status().isOk());

        MockHttpSession studentSession = (MockHttpSession) mvc.perform(post("/api/auth/demo-login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + student + "}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(studentSession)).andExpect(jsonPath("$.roles[0]").value("STUDENT"));
        mvc.perform(get("/api/users").session(studentSession)).andExpect(status().isForbidden());
    }

    @Test
    void renameRolesDeactivateAndActivateAreStored() throws Exception {
        long id = create("change-" + tag + "@vjti.ac.in", "[\"FACULTY\"]");

        mvc.perform(patch("/api/users/" + id).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Renamed Person\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/users/" + id + "/roles").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[\"INVIGILATOR\"]}")).andExpect(status().isOk());
        mvc.perform(post("/api/users/" + id + "/deactivate").session(admin).with(csrf())).andExpect(status().isOk());

        assertThat(jdbc.sql("SELECT full_name || '/' || status FROM app_user WHERE id = ?").param(id)
                .query(String.class).single()).isEqualTo("Renamed Person/DEACTIVATED");
        assertThat(roles(id)).containsExactly("INVIGILATOR");

        mvc.perform(post("/api/users/" + id + "/activate").session(admin).with(csrf()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void anAdministratorCannotChangeTheirOwnRoles() throws Exception {
        mvc.perform(put("/api/users/" + adminId + "/roles").session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"STUDENT\"]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SELF_ROLE_CHANGE"));
        assertThat(roles(adminId)).containsExactly("ADMIN");
    }

    private long create(String email, String roles) throws Exception {
        String json = mvc.perform(post("/api/users").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, roles)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    private static String body(String email, String roles) {
        return "{\"email\":\"" + email + "\",\"fullName\":\"Test person\",\"roles\":" + roles + "}";
    }

    private List<String> roles(long id) {
        return jdbc.sql("SELECT role FROM user_role WHERE user_id = ? ORDER BY role").param(id).query(String.class).list();
    }
}
