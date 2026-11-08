package com.slotforge.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** The planning inputs through the API, with results read back from PostgreSQL. */
@ActiveProfiles("local")
@AutoConfigureMockMvc
class ExamSetupCrudTest extends PostgresIntegrationTest {

    private static final String TWO_A_DAY = "\"weekdays\":[1,2,3,4,5],\"times\":[{\"start\":\"10:00\",\"end\":\"13:00\"},{\"start\":\"14:00\",\"end\":\"17:00\"}]";

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
    void slotsGenerateRepeatablyAndStayInsideTheSession() throws Exception {
        long session = createSession();

        // Monday to Sunday, but weekends are a gap, so five days at two slots each.
        send(post("/api/exam-sessions/" + session + "/time-slots/generate"), template(day(0), day(6), TWO_A_DAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(10))
                .andExpect(jsonPath("$.skipped").value(0));
        assertThat(count("SELECT count(*) FROM time_slot WHERE session_id = ?", session)).isEqualTo(10);

        send(post("/api/exam-sessions/" + session + "/time-slots/generate"), template(day(0), day(6), TWO_A_DAY))
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.skipped").value(10));

        // Overlapping without matching exactly is still skipped.
        send(post("/api/exam-sessions/" + session + "/time-slots/generate"),
                template(day(0), day(0), "\"weekdays\":[1],\"times\":[{\"start\":\"12:00\",\"end\":\"15:00\"}]"))
                .andExpect(jsonPath("$.created").value(0));

        send(post("/api/exam-sessions/" + session + "/time-slots/generate"), template(day(0), day(54), TWO_A_DAY))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("OUTSIDE_SESSION"));
        send(post("/api/exam-sessions/" + session + "/time-slots/generate"),
                template(day(0), day(0), "\"weekdays\":[1],\"times\":[{\"start\":\"13:00\",\"end\":\"10:00\"}]"))
                .andExpect(jsonPath("$.code").value("INVALID_TIME_WINDOW"));

        long slot = jdbc.sql("SELECT min(id) FROM time_slot WHERE session_id = ?").param(session).query(Long.class).single();
        send(patch("/api/time-slots/" + slot + "/status"), "{\"status\":\"BLOCKED\"}")
                .andExpect(jsonPath("$.code").value("REASON_REQUIRED"));
        send(patch("/api/time-slots/" + slot + "/status"), "{\"status\":\"BLOCKED\",\"reason\":\"Holiday\"}")
                .andExpect(jsonPath("$.status").value("BLOCKED"));
        send(patch("/api/time-slots/" + slot + "/status"), "{\"status\":\"OPEN\"}")
                .andExpect(jsonPath("$.blockReason").doesNotExist());

        // The session cannot shrink past a slot it holds.
        send(put("/api/exam-sessions/" + session), session("Session " + tag, day(1), day(11)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SLOTS_OUTSIDE_DATES"));
    }

    @Test
    void papersComeFromTheCurriculumAndEnrolmentDecidesReadiness() throws Exception {
        long session = createSession();
        long department = insert("INSERT INTO department (code, name) VALUES (?, ?) RETURNING id", "D" + tag, "Department " + tag);
        long course = insert("INSERT INTO course (department_id, code, name, total_semesters) VALUES (?, ?, 'P', 4) RETURNING id", department, "C" + tag);
        long curriculum = insert("INSERT INTO curriculum (course_id, name, effective_from_year) VALUES (?, 'S', 2024) RETURNING id", course);
        long batch = insert("INSERT INTO batch (curriculum_id, name, admission_year, current_semester) VALUES (?, 'B', 2025, 3) RETURNING id", curriculum);
        long core = subject(department, "CO" + tag);
        long elective = subject(department, "EL" + tag);
        long otherSemester = subject(department, "OT" + tag);
        place(curriculum, core, 3, false);
        place(curriculum, elective, 3, true);
        place(curriculum, otherSemester, 1, false);
        long first = student(batch, "1");
        long second = student(batch, "2");
        long gone = student(batch, "3");
        jdbc.sql("UPDATE student SET active = FALSE WHERE id = ?").param(gone).update();

        String request = "{\"batchIds\":[" + batch + "],\"durationMinutes\":180}";
        send(post("/api/exam-sessions/" + session + "/examinations/from-curriculum"), request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.enrolled").value(2))
                .andExpect(jsonPath("$.electivesAwaitingStudents").value(1));

        mvc.perform(get("/api/exam-sessions/" + session).session(coordinator))
                .andExpect(jsonPath("$.progress.readyPapers").value(1))
                .andExpect(jsonPath("$.progress.papersWithoutStudents.length()").value(1))
                .andExpect(jsonPath("$.progress.latestVersionNo").value(0));

        long coreExam = exam(session, core);
        long electiveExam = exam(session, elective);
        assertThat(statusOf(coreExam)).isEqualTo("READY");
        assertThat(statusOf(electiveExam)).isEqualTo("DRAFT");
        assertThat(count("SELECT count(*) FROM examination WHERE session_id = ?", session)).isEqualTo(2);

        // Repeating changes nothing.
        send(post("/api/exam-sessions/" + session + "/examinations/from-curriculum"), request)
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.enrolled").value(0));

        send(post("/api/examinations/" + electiveExam + "/enrolments"), "{\"studentIds\":[" + first + "," + gone + "]}")
                .andExpect(jsonPath("$.added").value(1));
        assertThat(statusOf(electiveExam)).isEqualTo("READY");
        mvc.perform(get("/api/examinations/" + electiveExam + "/enrolments").param("q", "R1" + tag).session(coordinator))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].studentId").value(first))
                .andExpect(jsonPath("$.items[0].courseCode").value("C" + tag));
        mvc.perform(delete("/api/examinations/" + electiveExam + "/enrolments/" + first).session(coordinator).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(statusOf(electiveExam)).isEqualTo("DRAFT");
        mvc.perform(delete("/api/examinations/" + electiveExam + "/enrolments/" + second).session(coordinator).with(csrf()))
                .andExpect(status().isNotFound());

        send(put("/api/examinations/" + coreExam), "{\"durationMinutes\":120,\"earliestDate\":\"" + day(3) + "\",\"latestDate\":\"" + day(2) + "\"}")
                .andExpect(jsonPath("$.code").value("INVALID_WINDOW"));
        send(put("/api/examinations/" + coreExam), "{\"durationMinutes\":120,\"earliestDate\":\"" + day(5) + "\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("WEEKEND_DATE"));
        send(put("/api/examinations/" + coreExam), "{\"durationMinutes\":120,\"earliestDate\":\"" + day(2) + "\",\"latestDate\":\"" + day(3) + "\"}")
                .andExpect(jsonPath("$.durationMinutes").value(120))
                .andExpect(jsonPath("$.enrolled").value(2));

        // A session past planning accepts no changes, and cannot be deleted.
        jdbc.sql("UPDATE exam_session SET status = 'COMPLETED' WHERE id = ?").param(session).update();
        mvc.perform(delete("/api/exam-sessions/" + session).session(coordinator).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_LOCKED"));
        send(post("/api/examinations/" + coreExam + "/enrolments"), "{\"studentIds\":[" + first + "]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_LOCKED"));
    }

    @Test
    void impossibleDatesAndTimesAreRefused() throws Exception {
        send(post("/api/exam-sessions"), session("Past " + tag, LocalDate.now().minusDays(2).toString(), day(1)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PAST_DATE"));
        send(post("/api/exam-sessions"), session("Long " + tag, day(0), day(121)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_DATES"));
        send(post("/api/exam-sessions"), session("Back " + tag, day(5), day(1)))
                .andExpect(jsonPath("$.code").value("INVALID_DATES"));
        send(post("/api/exam-sessions"), "{\"name\":\"Many " + tag + "\",\"startDate\":\"" + day(0) + "\",\"endDate\":\"" + day(1)
                + "\",\"maxExamsPerStudentPerDay\":5,\"heavyGapDays\":-1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));

        send(post("/api/exam-sessions"), session("Saturday " + tag, day(5), day(11)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("WEEKEND_DATE"));

        // A session already under way: its old dates can stay, but no new slot can go in the past.
        String started = LocalDate.now().minusDays(5).toString();
        long running = insert("INSERT INTO exam_session (name, start_date, end_date) VALUES (?, CAST(? AS DATE), CAST(? AS DATE)) RETURNING id",
                "Running " + tag, started, day(10));
        send(put("/api/exam-sessions/" + running), session("Renamed " + tag, started, day(10))).andExpect(status().isOk());
        send(post("/api/exam-sessions/" + running + "/time-slots/generate"), template(LocalDate.now().minusDays(2).toString(), day(1), TWO_A_DAY))
                .andExpect(jsonPath("$.code").value("PAST_DATE"));
        send(post("/api/exam-sessions/" + running + "/time-slots/generate"),
                template(day(0), day(1), "\"weekdays\":[1],\"times\":[{\"start\":\"10:00\",\"end\":\"13:00\"},{\"start\":\"12:00\",\"end\":\"15:00\"}]"))
                .andExpect(jsonPath("$.code").value("INVALID_TIME_WINDOW"));

        for (String window : List.of("{\"start\":\"08:00\",\"end\":\"10:00\"}", "{\"start\":\"15:00\",\"end\":\"18:00\"}")) {
            send(post("/api/exam-sessions/" + running + "/time-slots/generate"), template(day(0), day(1), "\"weekdays\":[1],\"times\":[" + window + "]"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("OUTSIDE_EXAM_HOURS"));
        }
        send(post("/api/exam-sessions/" + running + "/time-slots/generate"),
                template(day(0), day(1), "\"weekdays\":[6],\"times\":[{\"start\":\"10:00\",\"end\":\"13:00\"}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aCancelledSessionIsFrozenUntilReopened() throws Exception {
        long session = createSession();

        send(patch("/api/exam-sessions/" + session + "/status"), "{\"status\":\"COMPLETED\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        send(patch("/api/exam-sessions/" + session + "/status"), "{\"status\":\"CANCELLED\"}")
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        send(post("/api/exam-sessions/" + session + "/time-slots/generate"), template(day(0), day(6), TWO_A_DAY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_LOCKED"));

        send(patch("/api/exam-sessions/" + session + "/status"), "{\"status\":\"PLANNING\"}")
                .andExpect(jsonPath("$.status").value("PLANNING"));
        send(post("/api/exam-sessions/" + session + "/time-slots/generate"), template(day(0), day(6), TWO_A_DAY))
                .andExpect(jsonPath("$.created").value(10));
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mvc.perform(request.session(coordinator).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** A date n days after a Monday at least a week away, so the API's no-past rule always passes. */
    private static String day(int n) {
        return LocalDate.now().plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).plusDays(n).toString();
    }

    private long createSession() throws Exception {
        String json = send(post("/api/exam-sessions"), session("Session " + tag, day(0), day(11)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Long.parseLong(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    private static String session(String name, String start, String end) {
        return "{\"name\":\"" + name + "\",\"startDate\":\"" + start + "\",\"endDate\":\"" + end
                + "\",\"maxExamsPerStudentPerDay\":1,\"heavyGapDays\":1}";
    }

    private static String template(String from, String to, String rest) {
        return "{\"from\":\"" + from + "\",\"to\":\"" + to + "\"," + rest + "}";
    }

    private long subject(long department, String code) {
        return insert("INSERT INTO subject (department_id, code, name) VALUES (?, ?, 'Paper') RETURNING id", department, code);
    }

    private void place(long curriculum, long subject, int semester, boolean elective) {
        jdbc.sql("INSERT INTO curriculum_subject (curriculum_id, subject_id, semester_no, is_elective) VALUES (?, ?, ?, ?)")
                .param(curriculum).param(subject).param(semester).param(elective).update();
    }

    private long student(long batch, String suffix) {
        return insert("INSERT INTO student (batch_id, roll_no, full_name, email) VALUES (?, ?, 'A Student', ?) RETURNING id",
                batch, "R" + suffix + tag, "r" + suffix + tag.toLowerCase() + "@vjti.ac.in");
    }

    private long exam(long session, long subject) {
        return jdbc.sql("SELECT id FROM examination WHERE session_id = ? AND subject_id = ?").param(session).param(subject).query(Long.class).single();
    }

    private String statusOf(long exam) {
        return jdbc.sql("SELECT status FROM examination WHERE id = ?").param(exam).query(String.class).single();
    }

    private long count(String sql, long id) {
        return jdbc.sql(sql).param(id).query(Long.class).single();
    }

    private long insert(String sql, Object... values) {
        var statement = jdbc.sql(sql);
        for (Object value : values) statement = statement.param(value);
        return statement.query(Long.class).single();
    }
}
