package com.slotforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/** Halls and students through the API, with every result read back from PostgreSQL. */
@ActiveProfiles("local")
@AutoConfigureMockMvc
class RosterCrudTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    private MockHttpSession coordinator;
    private String tag;

    @BeforeEach
    void signInAsCoordinator() throws Exception {
        tag = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        long id = insert("INSERT INTO app_user (email, full_name) VALUES (?, 'Coordinator') RETURNING id",
                "coord-" + tag.toLowerCase() + "@demo.slotforge.test");
        jdbc.sql("INSERT INTO user_role (user_id, role) VALUES (?, 'EXAM_COORDINATOR')").param(id).update();
        coordinator = (MockHttpSession) mvc.perform(post("/api/auth/demo-login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + id + "}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    @Test
    void hallsAreStoredFilteredAndDeleted() throws Exception {
        long hall = idOf(mvc.perform(post("/api/halls").session(coordinator).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(hall("h" + tag.toLowerCase(), 60, "AVAILABLE")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        assertThat(jdbc.sql("SELECT code || '/' || capacity || '/' || status FROM exam_hall WHERE id = ?").param(hall)
                .query(String.class).single()).isEqualTo("H" + tag + "/60/AVAILABLE");

        mvc.perform(post("/api/halls").session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(hall("H" + tag, 10, "AVAILABLE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));

        mvc.perform(put("/api/halls/" + hall).session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(hall("H" + tag, 75, "MAINTENANCE"))).andExpect(status().isOk());
        mvc.perform(get("/api/halls").param("q", "H" + tag).param("status", "MAINTENANCE").session(coordinator))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].capacity").value(75));

        mvc.perform(delete("/api/halls/" + hall).session(coordinator).with(csrf())).andExpect(status().isNoContent());
        assertThat(jdbc.sql("SELECT count(*) FROM exam_hall WHERE id = ?").param(hall).query(Long.class).single()).isZero();
    }

    @Test
    void studentsAreNormalisedAndCannotBeDeletedOnceEnrolled() throws Exception {
        long department = insert("INSERT INTO department (code, name) VALUES (?, ?) RETURNING id", "D" + tag, "Department " + tag);
        long course = insert("INSERT INTO course (department_id, code, name, total_semesters) VALUES (?, ?, 'P', 4) RETURNING id", department, "C" + tag);
        long curriculum = insert("INSERT INTO curriculum (course_id, name, effective_from_year) VALUES (?, 'S', 2024) RETURNING id", course);
        long batch = insert("INSERT INTO batch (curriculum_id, name, admission_year, current_semester) VALUES (?, 'B', 2025, 1) RETURNING id", curriculum);

        long student = idOf(mvc.perform(post("/api/students").session(coordinator).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(student(batch, "r" + tag.toLowerCase(), "A.Student" + tag + "@VJTI.ac.in")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseCode").value("C" + tag))
                .andReturn().getResponse().getContentAsString());

        assertThat(jdbc.sql("SELECT roll_no || '/' || email FROM student WHERE id = ?").param(student).query(String.class).single())
                .isEqualTo("R" + tag + "/a.student" + tag.toLowerCase() + "@vjti.ac.in");

        mvc.perform(post("/api/students").session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(student(batch, "R" + tag, "other" + tag + "@vjti.ac.in")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
        mvc.perform(post("/api/students").session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(student(999_999_999L, "X" + tag, "x" + tag + "@vjti.ac.in")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNKNOWN_BATCH"));

        long subject = insert("INSERT INTO subject (department_id, code, name) VALUES (?, ?, 'Paper') RETURNING id", department, "S" + tag);
        long session = insert("INSERT INTO exam_session (name, start_date, end_date) VALUES (?, DATE '2026-12-01', DATE '2026-12-20') RETURNING id", "Session " + tag);
        long exam = insert("INSERT INTO examination (session_id, subject_id, duration_minutes) VALUES (?, ?, 180) RETURNING id", session, subject);
        jdbc.sql("INSERT INTO examination_enrolment (examination_id, student_id) VALUES (?, ?)").param(exam).param(student).update();

        mvc.perform(get("/api/students/" + student).session(coordinator)).andExpect(jsonPath("$.enrolmentCount").value(1));
        mvc.perform(delete("/api/students/" + student).session(coordinator).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IN_USE"));

        jdbc.sql("DELETE FROM examination_enrolment WHERE examination_id = ?").param(exam).update();
        mvc.perform(delete("/api/students/" + student).session(coordinator).with(csrf())).andExpect(status().isNoContent());
    }

    private long insert(String sql, Object... values) {
        var statement = jdbc.sql(sql);
        for (Object value : values) statement = statement.param(value);
        return statement.query(Long.class).single();
    }

    private static String hall(String code, int capacity, String status) {
        return "{\"code\":\"" + code + "\",\"name\":\"Hall\",\"building\":\"Main\",\"floor\":1,\"capacity\":" + capacity + ",\"status\":\"" + status + "\"}";
    }

    private static String student(long batchId, String rollNo, String email) {
        return "{\"batchId\":" + batchId + ",\"rollNo\":\"" + rollNo + "\",\"fullName\":\"A Student\",\"email\":\"" + email + "\"}";
    }

    private static long idOf(String json) {
        return Long.parseLong(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }
}
