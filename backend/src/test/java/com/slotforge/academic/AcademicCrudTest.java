package com.slotforge.academic;

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

/** Departments and programmes through the API, with every result read back from PostgreSQL. */
@ActiveProfiles("local")
@AutoConfigureMockMvc
class AcademicCrudTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    private MockHttpSession coordinator;
    private String tag;

    @BeforeEach
    void signInAsCoordinator() throws Exception {
        tag = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        long id = jdbc.sql("INSERT INTO app_user (email, full_name) VALUES (?, 'Test coordinator') RETURNING id")
                .param("coord-" + tag.toLowerCase() + "@demo.slotforge.test").query(Long.class).single();
        jdbc.sql("INSERT INTO user_role (user_id, role) VALUES (?, 'EXAM_COORDINATOR')").param(id).update();
        coordinator = (MockHttpSession) mvc.perform(post("/api/auth/demo-login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + id + "}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    @Test
    void createsADepartmentAndProgrammeWithNormalisedCodes() throws Exception {
        long department = createDepartment("d" + tag.toLowerCase(), "Department " + tag);
        long course = createCourse(department, "bt-" + tag.toLowerCase(), 8);

        assertThat(jdbc.sql("SELECT code FROM department WHERE id = ?").param(department).query(String.class).single())
                .isEqualTo("D" + tag);
        assertThat(jdbc.sql("SELECT code || '/' || total_semesters FROM course WHERE id = ?").param(course)
                .query(String.class).single()).isEqualTo("BT-" + tag + "/8");

        mvc.perform(get("/api/courses").param("departmentId", String.valueOf(department)).session(coordinator))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].departmentCode").value("D" + tag));
        mvc.perform(get("/api/departments").param("q", "D" + tag).session(coordinator))
                .andExpect(jsonPath("$.items[0].courseCount").value(1));
    }

    @Test
    void duplicateCodesAreRefusedWhateverTheCase() throws Exception {
        createDepartment("D" + tag, "Department " + tag);

        mvc.perform(post("/api/departments").session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(department("d" + tag.toLowerCase(), "Another " + tag)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
    }

    @Test
    void aDepartmentWithProgrammesCannotBeDeleted() throws Exception {
        long department = createDepartment("D" + tag, "Department " + tag);
        long course = createCourse(department, "BT-" + tag, 8);

        mvc.perform(delete("/api/departments/" + department).session(coordinator).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IN_USE"));
        assertThat(jdbc.sql("SELECT count(*) FROM department WHERE id = ?").param(department).query(Long.class).single())
                .isEqualTo(1);

        mvc.perform(delete("/api/courses/" + course).session(coordinator).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/departments/" + department).session(coordinator).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(jdbc.sql("SELECT count(*) FROM department WHERE id = ?").param(department).query(Long.class).single())
                .isZero();
    }

    @Test
    void aProgrammeNeedsAnExistingDepartment() throws Exception {
        mvc.perform(post("/api/courses").session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(course(999_999_999L, "BT-" + tag, 8)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNKNOWN_DEPARTMENT"));
    }

    @Test
    void editingKeepsTheSameRow() throws Exception {
        long department = createDepartment("D" + tag, "Department " + tag);

        mvc.perform(put("/api/departments/" + department).session(coordinator).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(department("D" + tag, "Renamed " + tag)))
                .andExpect(status().isOk());
        assertThat(jdbc.sql("SELECT name FROM department WHERE id = ?").param(department).query(String.class).single())
                .isEqualTo("Renamed " + tag);
    }

    @Test
    void aCurriculumPlacesSubjectsWithinItsProgrammesSemesters() throws Exception {
        long department = createDepartment("D" + tag, "Department " + tag);
        long course = createCourse(department, "MCA-" + tag, 4);
        long maths = createSubject(department, "M-" + tag, true);
        long dsa = createSubject(department, "DS-" + tag, false);
        long curriculum = createCurriculum(course, "2024 scheme");

        mvc.perform(put("/api/curricula/" + curriculum + "/subjects").session(coordinator).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placements(maths + ",1,false", dsa + ",3,true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("M-" + tag))
                .andExpect(jsonPath("$[0].heavy").value(true))
                .andExpect(jsonPath("$[1].elective").value(true));
        assertThat(jdbc.sql("SELECT count(*) FROM curriculum_subject WHERE curriculum_id = ?").param(curriculum)
                .query(Long.class).single()).isEqualTo(2);

        // Semester 5 does not exist in a 4-semester programme, and a subject appears once.
        mvc.perform(put("/api/curricula/" + curriculum + "/subjects").session(coordinator).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(placements(maths + ",5,false")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SEMESTER_OUT_OF_RANGE"));
        mvc.perform(put("/api/curricula/" + curriculum + "/subjects").session(coordinator).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(placements(maths + ",1,false", maths + ",2,false")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SUBJECT"));

        // The programme cannot shrink below semester 3, which DS uses.
        mvc.perform(put("/api/courses/" + course).session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(course(department, "MCA-" + tag, 2)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SEMESTERS_IN_USE"));

        // A placed subject, and a department with subjects, cannot be deleted.
        mvc.perform(delete("/api/subjects/" + maths).session(coordinator).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/departments/" + department).session(coordinator))
                .andExpect(jsonPath("$.subjectCount").value(2));

        // Emptying the curriculum frees the subject.
        mvc.perform(put("/api/curricula/" + curriculum + "/subjects").session(coordinator).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(placements())).andExpect(status().isOk());
        mvc.perform(delete("/api/subjects/" + maths).session(coordinator).with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    void aBatchFollowsOneCurriculumWithinItsProgrammesSemesters() throws Exception {
        long department = createDepartment("D" + tag, "Department " + tag);
        long course = createCourse(department, "MCA-" + tag, 4);
        long curriculum = createCurriculum(course, "2024 scheme");
        long batch = idOf(mvc.perform(post("/api/batches").session(coordinator).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(batch(curriculum, "2025-27", 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseCode").value("MCA-" + tag))
                .andExpect(jsonPath("$.totalSemesters").value(4))
                .andReturn().getResponse().getContentAsString());

        assertThat(jdbc.sql("SELECT name || '/' || admission_year || '/' || current_semester FROM batch WHERE id = ?")
                .param(batch).query(String.class).single()).isEqualTo("2025-27/2025/3");
        mvc.perform(get("/api/batches").param("courseId", String.valueOf(course)).session(coordinator))
                .andExpect(jsonPath("$.totalItems").value(1));

        // Duplicate name, a semester the programme lacks, and a change of curriculum are all refused.
        mvc.perform(post("/api/batches").session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(batch(curriculum, "2025-27", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
        mvc.perform(put("/api/batches/" + batch).session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(batch(curriculum, "2025-27", 5)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SEMESTER_OUT_OF_RANGE"));
        long other = createCurriculum(course, "2026 scheme");
        mvc.perform(put("/api/batches/" + batch).session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(batch(other, "2025-27", 3)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CURRICULUM_FIXED"));

        // Promotion is an ordinary edit. What the batch uses cannot be pulled out from under it.
        mvc.perform(put("/api/batches/" + batch).session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(batch(curriculum, "2025-27", 4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentSemester").value(4));
        mvc.perform(delete("/api/curricula/" + curriculum).session(coordinator).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IN_USE"));
        mvc.perform(put("/api/courses/" + course).session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(course(department, "MCA-" + tag, 2)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SEMESTERS_IN_USE"));

        // A batch with a student stays until the student is gone.
        long student = jdbc.sql("INSERT INTO student (batch_id, roll_no, full_name, email) VALUES (?, ?, 'A Student', ?) RETURNING id")
                .param(batch).param("R" + tag).param("r" + tag.toLowerCase() + "@vjti.ac.in").query(Long.class).single();
        mvc.perform(get("/api/batches/" + batch).session(coordinator)).andExpect(jsonPath("$.studentCount").value(1));
        mvc.perform(delete("/api/batches/" + batch).session(coordinator).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IN_USE"));
        jdbc.sql("DELETE FROM student WHERE id = ?").param(student).update();
        mvc.perform(delete("/api/batches/" + batch).session(coordinator).with(csrf())).andExpect(status().isNoContent());
    }

    private static String batch(long curriculumId, String name, int semester) {
        return "{\"curriculumId\":" + curriculumId + ",\"name\":\"" + name + "\",\"admissionYear\":2025,\"currentSemester\":" + semester + "}";
    }

    private long createSubject(long departmentId, String code, boolean heavy) throws Exception {
        return idOf(mvc.perform(post("/api/subjects").session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":" + departmentId + ",\"code\":\"" + code + "\",\"name\":\"Subject "
                                + code + "\",\"heavy\":" + heavy + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long createCurriculum(long courseId, String name) throws Exception {
        return idOf(mvc.perform(post("/api/curricula").session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":" + courseId + ",\"name\":\"" + name + "\",\"effectiveFromYear\":2024}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    /** Each entry is "subjectId,semester,elective". */
    private static String placements(String... entries) {
        StringBuilder json = new StringBuilder("{\"subjects\":[");
        for (int i = 0; i < entries.length; i++) {
            String[] parts = entries[i].split(",");
            if (i > 0) json.append(',');
            json.append("{\"subjectId\":").append(parts[0]).append(",\"semesterNo\":").append(parts[1])
                    .append(",\"elective\":").append(parts[2]).append('}');
        }
        return json.append("]}").toString();
    }

    private long createDepartment(String code, String name) throws Exception {
        return idOf(mvc.perform(post("/api/departments").session(coordinator).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(department(code, name)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long createCourse(long departmentId, String code, int semesters) throws Exception {
        return idOf(mvc.perform(post("/api/courses").session(coordinator).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(course(departmentId, code, semesters)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private static String department(String code, String name) {
        return "{\"code\":\"" + code + "\",\"name\":\"" + name + "\"}";
    }

    private static String course(long departmentId, String code, int semesters) {
        return "{\"departmentId\":" + departmentId + ",\"code\":\"" + code + "\",\"name\":\"Programme " + code
                + "\",\"totalSemesters\":" + semesters + "}";
    }

    private static long idOf(String json) {
        return Long.parseLong(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }
}
