package com.slotforge.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.ResultActions;

/** Press Generate against real PostgreSQL, then check what was stored with SQL that shares nothing with the engine. */
@ActiveProfiles("local")
@AutoConfigureMockMvc
class PlanningFlowTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    private MockHttpSession coordinator;
    private MockHttpSession headOfDepartment;
    private long actor;
    private String tag;
    private long session;
    private long batch;
    private long department;

    @BeforeEach
    void setUp() throws Exception {
        tag = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        actor = createUser("coord", "EXAM_COORDINATOR");
        coordinator = signIn(actor);
        headOfDepartment = signIn(createUser("hod", "HOD"));

        department = insert("INSERT INTO department (code, name) VALUES (?, ?) RETURNING id", "D" + tag, "Department " + tag);
        long course = insert("INSERT INTO course (department_id, code, name, total_semesters) VALUES (?, ?, 'P', 4) RETURNING id", department, "C" + tag);
        long curriculum = insert("INSERT INTO curriculum (course_id, name, effective_from_year) VALUES (?, 'S', 2024) RETURNING id", course);
        batch = insert("INSERT INTO batch (curriculum_id, name, admission_year, current_semester) VALUES (?, 'B', 2025, 1) RETURNING id", curriculum);
        session = insert("INSERT INTO exam_session (name, start_date, end_date) VALUES (?, DATE '2026-12-07', DATE '2026-12-19') RETURNING id", "Session " + tag);
        insert("INSERT INTO exam_hall (code, name, building, capacity) VALUES (?, 'Hall', 'Main', 3) RETURNING id", "H1" + tag);
        insert("INSERT INTO exam_hall (code, name, building, capacity) VALUES (?, 'Hall', 'Main', 3) RETURNING id", "H2" + tag);
        for (int day = 0; day < 3; day++) {
            jdbc.sql("INSERT INTO time_slot (session_id, slot_date, start_time, end_time) VALUES (?, DATE '2026-12-07' + CAST(? AS INT) * 5, TIME '10:00', TIME '13:00')")
                    .param(session).param(day).update();
        }
    }

    @Test
    void generatesAndStoresAClashFreeTimetableWithEveryStudentSeated() throws Exception {
        long first = student("1");
        long second = student("2");
        long third = student("3");
        long other = student("4");
        long maths = exam("MA", first, second, third);
        long physics = exam("PH", first, second);
        long chemistry = exam("CH", other);

        long version = idFrom(run().andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.examsScheduled").value(3))
                .andExpect(jsonPath("$.versionNo").value(1))
                .andExpect(jsonPath("$.unplaced.length()").value(0)));

        assertThat(count("SELECT count(*) FROM exam_schedule WHERE version_id = ?", version)).isEqualTo(3);
        assertThat(count("""
                SELECT count(*) FROM exam_schedule a
                JOIN exam_schedule b ON b.version_id = a.version_id AND b.time_slot_id = a.time_slot_id AND a.examination_id < b.examination_id
                JOIN examination_enrolment ea ON ea.examination_id = a.examination_id
                JOIN examination_enrolment eb ON eb.examination_id = b.examination_id AND eb.student_id = ea.student_id
                WHERE a.version_id = ?""", version)).as("student clashes").isZero();
        assertThat(count("""
                SELECT count(*) FROM student_hall_assignment p JOIN exam_schedule e ON e.id = p.exam_schedule_id WHERE e.version_id = ?""", version))
                .as("every enrolment seated exactly once").isEqualTo(6);
        assertThat(count("SELECT coalesce(sum(seats_allocated), 0) FROM hall_allocation WHERE version_id = ?", version)).isEqualTo(6);

        mvc.perform(get("/api/schedule-versions/" + version + "/entries").session(coordinator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.examinationId == %d)].students".formatted(maths)).value(3))
                .andExpect(jsonPath("$[?(@.examinationId == %d)].conflicts[0].examinationId".formatted(maths)).value((int) physics))
                .andExpect(jsonPath("$[?(@.examinationId == %d)].conflicts[0].sharedStudents".formatted(maths)).value(2))
                .andExpect(jsonPath("$[?(@.examinationId == %d)].conflicts[*]".formatted(chemistry)).isEmpty());

        // Generating again keeps the first draft and adds a second.
        run().andExpect(status().isCreated()).andExpect(jsonPath("$.versionNo").value(2));
        mvc.perform(get("/api/exam-sessions/" + session + "/schedule-versions").session(coordinator))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].versionNo").value(2))
                .andExpect(jsonPath("$[0].examsScheduled").value(3));

        mvc.perform(delete("/api/schedule-versions/" + version).session(coordinator).with(csrf())).andExpect(status().isNoContent());
        assertThat(count("SELECT count(*) FROM hall_allocation WHERE version_id = ?", version)).isZero();

        // Deleting the session takes the remaining draft, papers, slots and runs with it.
        mvc.perform(delete("/api/exam-sessions/" + session).session(coordinator).with(csrf())).andExpect(status().isNoContent());
        for (String table : List.of("schedule_version", "examination", "time_slot", "scheduling_run")) {
            assertThat(count("SELECT count(*) FROM " + table + " WHERE session_id = ?", session)).as(table).isZero();
        }
    }

    @Test
    void aPartialTimetableExplainsItselfAndGuardsHold() throws Exception {
        mvc.perform(post("/api/exam-sessions/" + session + "/planning/runs").session(coordinator).with(csrf()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NOTHING_TO_SCHEDULE"));

        long shared = student("1");
        exam("MA", shared);
        exam("PH", shared);
        jdbc.sql("""
                UPDATE time_slot SET status = 'BLOCKED', block_reason = 'Holiday'
                WHERE session_id = ? AND id <> (SELECT min(id) FROM time_slot WHERE session_id = ?)""")
                .param(session).param(session).update();

        long version = idFrom(run().andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.examsScheduled").value(1))
                .andExpect(jsonPath("$.unplaced.length()").value(1)));
        mvc.perform(get("/api/exam-sessions/" + session + "/schedule-versions").session(coordinator))
                .andExpect(jsonPath("$[0].unplaced[0].blockers.length()").value(2));

        // Nothing incomplete can be approved.
        mvc.perform(post("/api/schedule-versions/" + version + "/review").session(headOfDepartment).with(csrf()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNPLACED_PAPERS"));

        // A second run cannot start while one is active.
        long running = insert("INSERT INTO scheduling_run (session_id, kind, algorithm, parameters, started_by) VALUES (?, 'GENERATE', 'DSATUR', '{}', ?) RETURNING id", session, actor);
        run().andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RUN_ALREADY_ACTIVE"));
        jdbc.sql("UPDATE scheduling_run SET status = 'FAILED' WHERE id = ?").param(running).update();

        // Only a draft can be deleted.
        jdbc.sql("UPDATE schedule_version SET status = 'REVIEWED' WHERE id = ?").param(version).update();
        mvc.perform(delete("/api/schedule-versions/" + version).session(coordinator).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_NOT_DRAFT"));
    }

    @Test
    void aTimetableIsApprovedThenPublishedAndCanBeReplaced() throws Exception {
        exam("MA", student("1"));
        long first = idFrom(run().andExpect(status().isCreated()));
        mvc.perform(get("/api/schedule-versions/awaiting-approval").session(headOfDepartment))
                .andExpect(jsonPath("$[?(@.versionId == %d)].sessionId".formatted(first)).value((int) session));

        // Nothing is published without approval, and an unhappy reviewer can say why.
        publish(first).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("VERSION_NOT_REVIEWED"));
        mvc.perform(post("/api/schedule-versions/" + first + "/return").session(headOfDepartment).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"Move this earlier\"}"))
                .andExpect(status().isNoContent());
        versionsShow("DRAFT").andExpect(jsonPath("$[0].reviewComment").value("Move this earlier"));

        approve(first).andExpect(status().isNoContent());
        mvc.perform(get("/api/schedule-versions/awaiting-approval").session(headOfDepartment))
                .andExpect(jsonPath("$[?(@.versionId == %d)]".formatted(first)).isEmpty());
        versionsShow("REVIEWED").andExpect(jsonPath("$[0].reviewedBy").value("Reviewer")).andExpect(jsonPath("$[0].reviewComment").doesNotExist());
        approve(first).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_NOT_DRAFT"));

        publish(first).andExpect(status().isNoContent());
        versionsShow("PUBLISHED").andExpect(jsonPath("$[0].publishedAt").exists());
        assertThat(jdbc.sql("SELECT status FROM exam_session WHERE id = ?").param(session).query(String.class).single()).isEqualTo("PUBLISHED");
        run().andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SESSION_LOCKED"));

        // Reopening keeps the old timetable official until a new one replaces it.
        mvc.perform(patch("/api/exam-sessions/" + session + "/status").session(coordinator).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PLANNING\"}"))
                .andExpect(status().isOk());
        versionsShow("PUBLISHED");
        long second = idFrom(run().andExpect(status().isCreated()));
        approve(second).andExpect(status().isNoContent());
        publish(second).andExpect(status().isNoContent());
        versionsShow("PUBLISHED").andExpect(jsonPath("$[1].status").value("SUPERSEDED"));
    }

    @Test
    void aTimetableThatHasGoneOutOfDateCannotBeApprovedOrPublished() throws Exception {
        long first = student("1");
        long second = student("2");
        long maths = exam("MA", first);
        exam("PH", second);
        long version = idFrom(run().andExpect(status().isCreated()));
        assertThat(count("SELECT count(DISTINCT time_slot_id) FROM exam_schedule WHERE version_id = ?", version)).as("share one slot").isEqualTo(1);
        assertThat(problems(version)).isEqualTo("[]");
        approve(version).andExpect(status().isNoContent());

        // A late enrolment puts the second student in both papers at once, with no seat in the new one.
        mvc.perform(post("/api/examinations/" + maths + "/enrolments").session(coordinator).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"studentIds\":[" + second + "]}"))
                .andExpect(status().isOk());
        assertThat(problems(version))
                .contains("MA" + tag + " has 1 student without a seat.")
                .contains("MA" + tag + " and PH" + tag + " are at the same time for 1 student.");

        publish(version).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("TIMETABLE_OUT_OF_DATE"));
        versionsShow("REVIEWED");
        assertThat(jdbc.sql("SELECT status FROM exam_session WHERE id = ?").param(session).query(String.class).single()).isEqualTo("PLANNING");

        // Generating again gives a timetable that holds.
        long replacement = idFrom(run().andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("SUCCEEDED")));
        assertThat(problems(replacement)).isEqualTo("[]");
        approve(replacement).andExpect(status().isNoContent());
        publish(replacement).andExpect(status().isNoContent());
    }

    @Test
    void everyLaterChangeToAPaperSlotOrHallIsCaught() throws Exception {
        long maths = exam("MA", student("1"));
        long version = idFrom(run().andExpect(status().isCreated()));
        long slot = jdbc.sql("SELECT time_slot_id FROM exam_schedule WHERE version_id = ?").param(version).query(Long.class).single();

        change("UPDATE time_slot SET status = 'BLOCKED', block_reason = 'Holiday' WHERE id = ?", slot);
        assertThat(problems(version)).contains("MA" + tag + " is in a slot that is now blocked");
        approve(version).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("TIMETABLE_OUT_OF_DATE"));
        versionsShow("DRAFT");
        change("UPDATE time_slot SET status = 'OPEN', block_reason = NULL WHERE id = ?", slot);

        change("UPDATE examination SET duration_minutes = 240 WHERE id = ?", maths);
        assertThat(problems(version)).contains("MA" + tag + " now needs 240 minutes, but its slot is 180.");
        change("UPDATE examination SET duration_minutes = 180 WHERE id = ?", maths);

        change("UPDATE examination SET earliest_date = DATE '2026-12-18' WHERE id = ?", maths);
        assertThat(problems(version)).contains("MA" + tag + " is outside its allowed dates.");
        change("UPDATE examination SET earliest_date = NULL WHERE id = ?", maths);

        long hall = jdbc.sql("SELECT hall_id FROM hall_allocation WHERE version_id = ?").param(version).query(Long.class).single();
        change("UPDATE exam_hall SET status = 'MAINTENANCE' WHERE id = ?", hall);
        assertThat(problems(version)).contains(" can no longer take MA" + tag + ".");
        change("UPDATE exam_hall SET status = 'AVAILABLE' WHERE id = ?", hall);

        assertThat(problems(version)).isEqualTo("[]");
        exam("PH", student("2"));
        assertThat(problems(version)).contains("PH" + tag + " is ready but not in this timetable.");
    }

    @Test
    void rulesTightenedAfterGeneratingAreCaught() throws Exception {
        jdbc.sql("INSERT INTO time_slot (session_id, slot_date, start_time, end_time) VALUES (?, DATE '2026-12-07', TIME '14:00', TIME '17:00')").param(session).update();
        jdbc.sql("UPDATE time_slot SET status = 'BLOCKED', block_reason = 'Holiday' WHERE session_id = ? AND slot_date <> DATE '2026-12-07'").param(session).update();
        change("UPDATE exam_session SET max_exams_per_student_per_day = 2 WHERE id = ?", session);
        long shared = student("1");
        exam("MA", shared);
        exam("PH", shared);
        long version = idFrom(run().andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("SUCCEEDED")));
        assertThat(problems(version)).isEqualTo("[]");

        change("UPDATE exam_session SET max_exams_per_student_per_day = 1 WHERE id = ?", session);
        assertThat(problems(version)).contains("1 student has too many papers on Mon, 7 Dec.");

        jdbc.sql("UPDATE subject SET is_heavy = TRUE WHERE department_id = ?").param(department).update();
        assertThat(problems(version)).contains("Heavy papers MA" + tag + " and PH" + tag + " are too close together for 1 student.");
    }

    private String problems(long version) throws Exception {
        return mvc.perform(get("/api/schedule-versions/" + version + "/violations").session(coordinator))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private void change(String sql, long id) {
        jdbc.sql(sql).param(id).update();
    }

    private ResultActions approve(long version) throws Exception {
        return mvc.perform(post("/api/schedule-versions/" + version + "/review").session(headOfDepartment).with(csrf()));
    }

    private ResultActions publish(long version) throws Exception {
        return mvc.perform(post("/api/schedule-versions/" + version + "/publish").session(coordinator).with(csrf()));
    }

    /** Asserts the status of the newest version. */
    private ResultActions versionsShow(String status) throws Exception {
        return mvc.perform(get("/api/exam-sessions/" + session + "/schedule-versions").session(coordinator))
                .andExpect(jsonPath("$[0].status").value(status));
    }

    private long createUser(String prefix, String role) {
        long id = insert("INSERT INTO app_user (email, full_name) VALUES (?, ?) RETURNING id", prefix + "-" + tag.toLowerCase() + "@demo.slotforge.test",
                "HOD".equals(role) ? "Reviewer" : "Coordinator");
        jdbc.sql("INSERT INTO user_role (user_id, role) VALUES (?, ?)").param(id).param(role).update();
        return id;
    }

    private MockHttpSession signIn(long userId) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/demo-login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + userId + "}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private ResultActions run() throws Exception {
        return mvc.perform(post("/api/exam-sessions/" + session + "/planning/runs").session(coordinator).with(csrf()));
    }

    private static long idFrom(ResultActions result) throws Exception {
        return Long.parseLong(result.andReturn().getResponse().getContentAsString().replaceAll(".*\"versionId\":(\\d+).*", "$1"));
    }

    private long student(String suffix) {
        return insert("INSERT INTO student (batch_id, roll_no, full_name, email) VALUES (?, ?, 'A Student', ?) RETURNING id",
                batch, "R" + suffix + tag, "r" + suffix + tag.toLowerCase() + "@vjti.ac.in");
    }

    private long exam(String prefix, long... students) {
        long subject = insert("INSERT INTO subject (department_id, code, name) VALUES (?, ?, 'Paper') RETURNING id", department, prefix + tag);
        long exam = insert("INSERT INTO examination (session_id, subject_id, duration_minutes, status) VALUES (?, ?, 180, 'READY') RETURNING id", session, subject);
        for (long student : students) {
            jdbc.sql("INSERT INTO examination_enrolment (examination_id, student_id) VALUES (?, ?)").param(exam).param(student).update();
        }
        return exam;
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
