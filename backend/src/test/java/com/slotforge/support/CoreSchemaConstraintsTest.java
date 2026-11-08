package com.slotforge.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/** Proves the database itself rejects bad data, using raw SQL that bypasses all Java checks. */
@Transactional
class CoreSchemaConstraintsTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcClient jdbc;

    private long userId;
    private long sessionId;
    private long slotId;
    private long otherSlotId;
    private long hallId;
    private long otherHallId;
    private long examId;
    private long otherExamId;
    private long studentId;
    private long otherStudentId;
    private long versionId;
    private long scheduleId;
    private long otherScheduleId;

    private long insert(String sql, Object... params) {
        return jdbc.sql(sql + " RETURNING id").params(params).query(Long.class).single();
    }

    private int run(String sql, Object... params) {
        return jdbc.sql(sql).params(params).update();
    }

    private long count(String sql, Object... params) {
        return jdbc.sql(sql).params(params).query(Long.class).single();
    }

    private void assertRejected(String expected, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(DataAccessException.class).hasStackTraceContaining(expected);
    }

    private long subject(long departmentId, String code) {
        return insert("INSERT INTO subject (department_id, code, name) VALUES (?, ?, ?)", departmentId, code, code);
    }

    private long student(long batchId, String roll) {
        return insert("INSERT INTO student (batch_id, roll_no, full_name, email) VALUES (?, ?, ?, ?)",
                batchId, roll, roll, roll.toLowerCase() + "@example.test");
    }

    private long hall(String code) {
        return insert("INSERT INTO exam_hall (code, name, building, capacity) VALUES (?, ?, 'Main', 60)", code, code);
    }

    private long slot(String start, String end) {
        return insert("INSERT INTO time_slot (session_id, slot_date, start_time, end_time) "
                + "VALUES (?, DATE '2026-12-01', ?::time, ?::time)", sessionId, start, end);
    }

    private long exam(long subjectId) {
        return insert("INSERT INTO examination (session_id, subject_id, duration_minutes) VALUES (?, ?, 180)",
                sessionId, subjectId);
    }

    private long schedule(long exam, long slot) {
        return insert("INSERT INTO exam_schedule (version_id, examination_id, time_slot_id) VALUES (?, ?, ?)",
                versionId, exam, slot);
    }

    private long allocate(long schedule, long slot, long hall, int seats, int capacity) {
        return insert("INSERT INTO hall_allocation "
                + "(exam_schedule_id, version_id, time_slot_id, hall_id, seats_allocated, capacity_snapshot) "
                + "VALUES (?, ?, ?, ?, ?, ?)", schedule, versionId, slot, hall, seats, capacity);
    }

    private void place(long schedule, long exam, long allocation, long student) {
        run("INSERT INTO student_hall_assignment (exam_schedule_id, examination_id, hall_allocation_id, student_id) "
                + "VALUES (?, ?, ?, ?)", schedule, exam, allocation, student);
    }

    @BeforeEach
    void createOneOfEverything() {
        userId = insert("INSERT INTO app_user (email, full_name) VALUES ('owner@example.test', 'Owner')");
        long department = insert("INSERT INTO department (code, name) VALUES ('MCA', 'Computer Applications')");
        long course = insert("INSERT INTO course (department_id, code, name, total_semesters) "
                + "VALUES (?, 'MCA', 'MCA', 4)", department);
        long curriculum = insert("INSERT INTO curriculum (course_id, name, effective_from_year) "
                + "VALUES (?, 'MCA 2025', 2025)", course);
        long batch = insert("INSERT INTO batch (curriculum_id, name, admission_year, current_semester) "
                + "VALUES (?, 'MCA 2025-27', 2025, 1)", curriculum);
        studentId = student(batch, "S1");
        otherStudentId = student(batch, "S2");
        hallId = hall("H1");
        otherHallId = hall("H2");
        sessionId = insert("INSERT INTO exam_session (name, start_date, end_date) "
                + "VALUES ('Dec 2026', DATE '2026-12-01', DATE '2026-12-20')");
        slotId = slot("10:00", "13:00");
        otherSlotId = slot("14:00", "17:00");
        examId = exam(subject(department, "MCA101"));
        otherExamId = exam(subject(department, "MCA102"));
        run("INSERT INTO examination_enrolment (examination_id, student_id) VALUES (?, ?)", examId, studentId);
        versionId = insert("INSERT INTO schedule_version (session_id, version_no, created_by) VALUES (?, 1, ?)",
                sessionId, userId);
        scheduleId = schedule(examId, slotId);
        otherScheduleId = schedule(otherExamId, slotId);
    }

    @Test
    void slotsInASessionCannotOverlap() {
        assertRejected("ex_time_slot_overlap", () -> slot("12:00", "15:00"));
    }

    @Test
    void adjacentSlotsAreAllowed() {
        assertThat(slot("13:00", "14:00")).isPositive();
    }

    @Test
    void anExamAppearsOncePerVersion() {
        assertRejected("uq_exam_once_per_version", () -> schedule(examId, otherSlotId));
    }

    @Test
    void aHallCannotHoldTwoExamsInOneSlot() {
        allocate(scheduleId, slotId, hallId, 30, 60);

        assertRejected("uq_hall_no_double_booking", () -> allocate(otherScheduleId, slotId, hallId, 30, 60));
    }

    @Test
    void aLargeExamMaySplitAcrossHalls() {
        allocate(scheduleId, slotId, hallId, 60, 60);

        assertThat(allocate(scheduleId, slotId, otherHallId, 10, 60)).isPositive();
    }

    @Test
    void seatsCannotExceedCapacity() {
        assertRejected("ck_hall_alloc_capacity", () -> allocate(scheduleId, slotId, hallId, 61, 60));
    }

    @Test
    void aCopiedSlotMustMatchItsScheduledExam() {
        assertRejected("fk_hall_alloc_schedule", () -> allocate(scheduleId, otherSlotId, hallId, 30, 60));
    }

    @Test
    void aStudentMustBeEnrolledToBePlacedInAHall() {
        long allocation = allocate(scheduleId, slotId, hallId, 30, 60);

        assertRejected("fk_sha_enrolment", () -> place(scheduleId, examId, allocation, otherStudentId));
    }

    @Test
    void aPlacementMustNameTheExamThatIsScheduled() {
        long allocation = allocate(scheduleId, slotId, hallId, 30, 60);
        run("INSERT INTO examination_enrolment (examination_id, student_id) VALUES (?, ?)", otherExamId, studentId);

        assertRejected("fk_sha_schedule", () -> place(scheduleId, otherExamId, allocation, studentId));
    }

    @Test
    void aStudentSitsAnExamInOneHallOnly() {
        long first = allocate(scheduleId, slotId, hallId, 30, 60);
        long second = allocate(scheduleId, slotId, otherHallId, 30, 60);
        place(scheduleId, examId, first, studentId);

        assertRejected("student_hall_assignment_pkey", () -> place(scheduleId, examId, second, studentId));
    }

    @Test
    void enrolmentCannotBeRemovedWhileAStudentIsPlaced() {
        place(scheduleId, examId, allocate(scheduleId, slotId, hallId, 30, 60), studentId);

        assertRejected("fk_sha_enrolment",
                () -> run("DELETE FROM examination_enrolment WHERE examination_id = ?", examId));
    }

    @Test
    void onlyOneRunCanBeActivePerSession() {
        String sql = "INSERT INTO scheduling_run (session_id, kind, algorithm, parameters, started_by) "
                + "VALUES (?, 'GENERATE', 'DSATUR', '{}', ?)";
        run(sql, sessionId, userId);

        assertRejected("uq_run_one_running", () -> run(sql, sessionId, userId));
    }

    @Test
    void onlyOneVersionCanBePublishedPerSession() {
        run("UPDATE schedule_version SET status = 'PUBLISHED' WHERE id = ?", versionId);

        assertRejected("uq_version_one_published", () -> run(
                "INSERT INTO schedule_version (session_id, version_no, status, created_by) "
                        + "VALUES (?, 2, 'PUBLISHED', ?)", sessionId, userId));
    }

    @Test
    void aFrozenVersionCannotChange() {
        long allocation = allocate(scheduleId, slotId, hallId, 30, 60);
        place(scheduleId, examId, allocation, studentId);
        run("UPDATE schedule_version SET status = 'PUBLISHED' WHERE id = ?", versionId);

        assertRejected("frozen", () -> run("UPDATE exam_schedule SET locked = TRUE WHERE id = ?", scheduleId));
    }

    @Test
    void aFrozenVersionCannotGainHalls() {
        run("UPDATE schedule_version SET status = 'REVIEWED' WHERE id = ?", versionId);

        assertRejected("frozen", () -> allocate(scheduleId, slotId, hallId, 30, 60));
    }

    @Test
    void placementsInAFrozenVersionCannotChange() {
        place(scheduleId, examId, allocate(scheduleId, slotId, hallId, 30, 60), studentId);
        run("UPDATE schedule_version SET status = 'SUPERSEDED' WHERE id = ?", versionId);

        assertRejected("frozen",
                () -> run("DELETE FROM student_hall_assignment WHERE exam_schedule_id = ?", scheduleId));
    }

    @Test
    void aFrozenVersionCannotBeDeleted() {
        run("UPDATE schedule_version SET status = 'PUBLISHED' WHERE id = ?", versionId);

        assertRejected("frozen", () -> run("DELETE FROM schedule_version WHERE id = ?", versionId));
    }

    @Test
    void aDraftVersionDeletesWithEverythingInIt() {
        place(scheduleId, examId, allocate(scheduleId, slotId, hallId, 30, 60), studentId);

        run("DELETE FROM schedule_version WHERE id = ?", versionId);

        // Scoped to this draft: other database tests commit their own timetables to the same tables.
        assertThat(count("SELECT count(*) FROM exam_schedule WHERE version_id = ?", versionId)).isZero();
        assertThat(count("SELECT count(*) FROM hall_allocation WHERE version_id = ?", versionId)).isZero();
        assertThat(count("SELECT count(*) FROM student_hall_assignment WHERE exam_schedule_id = ?", scheduleId)).isZero();
    }

    @Test
    void emailsMustBeLowerCase() {
        assertRejected("ck_app_user_email_lower",
                () -> run("INSERT INTO app_user (email, full_name) VALUES ('Mixed@Example.test', 'X')"));
    }

    @Test
    void auditDetailsMustBeJson() {
        assertRejected("ck_audit_details_json", () -> run(
                "INSERT INTO audit_log (action, entity_type, details) VALUES ('X', 'Y', 'not json')"));
    }

    @Test
    void aDepartmentInUseCannotBeDeleted() {
        assertRejected("foreign key", () -> run("DELETE FROM department"));
    }
}
