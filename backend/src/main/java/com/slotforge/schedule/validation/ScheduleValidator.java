package com.slotforge.schedule.validation;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Compares a stored timetable with today's papers, students, slots and halls, in plain SQL that shares
 * nothing with the engine. Papers, slots and halls stay editable after a timetable is generated, so a
 * timetable that was correct then can be wrong now.
 */
@Service
@RequiredArgsConstructor
public class ScheduleValidator {

    private record Rule(String sql, RowMapper<String> message) {
    }

    private static final String CONTEXT = """
            WITH rules AS (
                SELECT s.id AS session_id, s.max_exams_per_student_per_day AS max_per_day, s.heavy_gap_days AS heavy_gap,
                       s.students_per_invigilator AS per_invigilator, s.max_duties_per_invigilator AS max_duties
                FROM exam_session s JOIN schedule_version v ON v.session_id = s.id
                WHERE v.id = :version
            ), placed AS (
                SELECT es.id AS schedule_id, es.examination_id, su.code, su.is_heavy, x.duration_minutes, x.earliest_date, x.latest_date,
                       t.id AS slot_id, t.slot_date, t.start_time, t.end_time, t.status AS slot_status,
                       CAST(EXTRACT(EPOCH FROM (t.end_time - t.start_time)) / 60 AS INT) AS slot_minutes,
                       to_char(t.slot_date, 'FMDy, FMDD Mon') AS day
                FROM exam_schedule es
                JOIN examination x ON x.id = es.examination_id
                JOIN subject su ON su.id = x.subject_id
                JOIN time_slot t ON t.id = es.time_slot_id
                WHERE es.version_id = :version
            ), roster AS (
                SELECT p.*, e.student_id
                FROM placed p
                JOIN examination_enrolment e ON e.examination_id = p.examination_id
                JOIN student s ON s.id = e.student_id AND s.active
            ), booked AS (
                SELECT es.id AS schedule_id, es.examination_id, su.code, os.name AS session_name, t.slot_date, t.start_time, t.end_time
                FROM schedule_version v
                JOIN exam_session os ON os.id = v.session_id AND os.status <> 'CANCELLED'
                JOIN exam_schedule es ON es.version_id = v.id
                JOIN examination x ON x.id = es.examination_id
                JOIN subject su ON su.id = x.subject_id
                JOIN time_slot t ON t.id = es.time_slot_id
                WHERE v.status = 'PUBLISHED' AND v.session_id <> (SELECT session_id FROM rules)
            )
            """;

    private static final List<Rule> RULES = List.of(
            new Rule("""
                    SELECT su.code
                    FROM examination x JOIN subject su ON su.id = x.subject_id
                    WHERE x.session_id = (SELECT session_id FROM rules) AND x.status = 'READY'
                      AND EXISTS (SELECT 1 FROM examination_enrolment e JOIN student s ON s.id = e.student_id AND s.active WHERE e.examination_id = x.id)
                      AND NOT EXISTS (SELECT 1 FROM placed p WHERE p.examination_id = x.id)
                    ORDER BY su.code""",
                    (row, index) -> row.getString(1) + " is ready but not in this timetable."),
            new Rule("""
                    SELECT code, count(*)
                    FROM roster r
                    WHERE NOT EXISTS (SELECT 1 FROM student_hall_assignment a WHERE a.exam_schedule_id = r.schedule_id AND a.student_id = r.student_id)
                    GROUP BY code ORDER BY code""",
                    (row, index) -> row.getString(1) + " has " + students(row.getInt(2)) + " without a seat."),
            new Rule("""
                    SELECT a.code, b.code, count(*)
                    FROM roster a JOIN roster b ON b.student_id = a.student_id AND b.slot_id = a.slot_id AND a.examination_id < b.examination_id
                    GROUP BY a.code, b.code ORDER BY a.code, b.code""",
                    (row, index) -> row.getString(1) + " and " + row.getString(2) + " are at the same time for " + students(row.getInt(3)) + "."),
            new Rule("""
                    SELECT day, count(*)
                    FROM (SELECT student_id, slot_date, day FROM roster GROUP BY student_id, slot_date, day
                          HAVING count(DISTINCT slot_id) > (SELECT max_per_day FROM rules)) crowded
                    GROUP BY slot_date, day ORDER BY slot_date""",
                    (row, index) -> students(row.getInt(2)) + (row.getInt(2) == 1 ? " has" : " have") + " too many papers on " + row.getString(1) + "."),
            new Rule("""
                    SELECT a.code, b.code, count(*)
                    FROM roster a JOIN roster b ON b.student_id = a.student_id AND a.examination_id < b.examination_id AND a.slot_id <> b.slot_id
                    WHERE a.is_heavy AND b.is_heavy AND abs(a.slot_date - b.slot_date) <= (SELECT heavy_gap FROM rules)
                    GROUP BY a.code, b.code ORDER BY a.code, b.code""",
                    (row, index) -> "Heavy papers " + row.getString(1) + " and " + row.getString(2) + " are too close together for " + students(row.getInt(3)) + "."),
            new Rule("SELECT code, day FROM placed WHERE slot_status <> 'OPEN' ORDER BY code",
                    (row, index) -> row.getString(1) + " is in a slot that is now blocked (" + row.getString(2) + ")."),
            // A published timetable keeps its past papers, so only one still on its way is checked.
            new Rule("""
                    SELECT code, day FROM placed
                    WHERE slot_date + start_time < (now() AT TIME ZONE 'Asia/Kolkata')
                      AND (SELECT status FROM schedule_version WHERE id = :version) <> 'PUBLISHED'
                    ORDER BY code""",
                    (row, index) -> row.getString(1) + " is in a slot that has already passed (" + row.getString(2) + ")."),
            new Rule("SELECT code, duration_minutes, slot_minutes FROM placed WHERE duration_minutes > slot_minutes ORDER BY code",
                    (row, index) -> row.getString(1) + " now needs " + row.getInt(2) + " minutes, but its slot is " + row.getInt(3) + "."),
            new Rule("""
                    SELECT code FROM placed
                    WHERE slot_date NOT BETWEEN coalesce(earliest_date, slot_date) AND coalesce(latest_date, slot_date)
                    ORDER BY code""",
                    (row, index) -> row.getString(1) + " is outside its allowed dates."),
            new Rule("""
                    SELECT h.code, p.code
                    FROM hall_allocation a
                    JOIN exam_hall h ON h.id = a.hall_id
                    JOIN placed p ON p.schedule_id = a.exam_schedule_id
                    WHERE h.status <> 'AVAILABLE' OR h.capacity < a.seats_allocated
                       OR EXISTS (SELECT 1 FROM hall_unavailability u WHERE u.hall_id = h.id AND p.slot_date BETWEEN u.from_date AND u.to_date)
                    ORDER BY h.code, p.code""",
                    (row, index) -> row.getString(1) + " can no longer take " + row.getString(2) + "."),
            new Rule("""
                    SELECT DISTINCT h.code AS hall, p.code AS paper, b.code AS other, b.session_name, p.day
                    FROM placed p
                    JOIN hall_allocation a ON a.exam_schedule_id = p.schedule_id
                    JOIN exam_hall h ON h.id = a.hall_id
                    JOIN booked b ON b.slot_date = p.slot_date AND b.start_time < p.end_time AND p.start_time < b.end_time
                    JOIN hall_allocation theirs ON theirs.exam_schedule_id = b.schedule_id AND theirs.hall_id = a.hall_id
                    ORDER BY hall, paper, other""",
                    (row, index) -> row.getString(1) + " is already in use for " + row.getString(3) + " (" + row.getString(4) + ") when "
                            + row.getString(2) + " sits on " + row.getString(5) + "."),
            new Rule("""
                    SELECT r.code, b.code, b.session_name, count(DISTINCT r.student_id)
                    FROM roster r
                    JOIN booked b ON b.slot_date = r.slot_date AND b.start_time < r.end_time AND r.start_time < b.end_time
                    JOIN examination_enrolment e ON e.examination_id = b.examination_id AND e.student_id = r.student_id
                    GROUP BY r.code, b.code, b.session_name ORDER BY r.code, b.code""",
                    (row, index) -> row.getString(1) + " is at the same time as " + row.getString(2) + " (" + row.getString(3) + ") for "
                            + students(row.getInt(4)) + "."),
            new Rule("""
                    SELECT h.code, p.code, p.day, (a.seats_allocated + r.per_invigilator - 1) / r.per_invigilator, count(i.id),
                           (SELECT count(DISTINCT u.id) FROM app_user u JOIN user_role ur ON ur.user_id = u.id
                            WHERE u.status = 'ACTIVE' AND ur.role IN ('FACULTY', 'INVIGILATOR')),
                           (SELECT count(DISTINCT su.user_id) FROM staff_unavailability su WHERE p.slot_date BETWEEN su.from_date AND su.to_date)
                    FROM hall_allocation a
                    JOIN placed p ON p.schedule_id = a.exam_schedule_id
                    JOIN exam_hall h ON h.id = a.hall_id
                    CROSS JOIN rules r
                    LEFT JOIN invigilator_assignment i ON i.hall_allocation_id = a.id
                    GROUP BY a.id, a.seats_allocated, r.per_invigilator, h.code, p.code, p.day, p.slot_date
                    HAVING count(i.id) < (a.seats_allocated + r.per_invigilator - 1) / r.per_invigilator
                    ORDER BY h.code, p.code""",
                    (row, index) -> row.getString(1) + " needs " + row.getInt(4) + (row.getInt(4) == 1 ? " invigilator" : " invigilators") + " for "
                            + row.getString(2) + " on " + row.getString(3) + ", but has " + row.getInt(5) + ". Of " + row.getInt(6) + " staff, "
                            + row.getInt(7) + " cannot invigilate that day and the rest are on duty elsewhere or at their duty limit."),
            new Rule("""
                    SELECT u.full_name, p.code, h.code, p.day
                    FROM invigilator_assignment i
                    JOIN hall_allocation a ON a.id = i.hall_allocation_id
                    JOIN placed p ON p.schedule_id = a.exam_schedule_id
                    JOIN exam_hall h ON h.id = a.hall_id
                    JOIN app_user u ON u.id = i.user_id
                    WHERE i.version_id = :version
                      AND (u.status <> 'ACTIVE'
                           OR NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id = u.id AND ur.role IN ('FACULTY', 'INVIGILATOR'))
                           OR EXISTS (SELECT 1 FROM staff_unavailability su WHERE su.user_id = u.id AND p.slot_date BETWEEN su.from_date AND su.to_date))
                    ORDER BY u.full_name, p.code""",
                    (row, index) -> row.getString(1) + " can no longer invigilate " + row.getString(2) + " in " + row.getString(3) + " on " + row.getString(4) + "."),
            new Rule("""
                    SELECT u.full_name, count(*), (SELECT max_duties FROM rules)
                    FROM invigilator_assignment i JOIN app_user u ON u.id = i.user_id
                    WHERE i.version_id = :version
                    GROUP BY u.id, u.full_name
                    HAVING count(*) > (SELECT max_duties FROM rules)
                    ORDER BY u.full_name""",
                    (row, index) -> row.getString(1) + " has " + row.getInt(2) + " duties, over the limit of " + row.getInt(3) + "."),
            new Rule("""
                    SELECT DISTINCT u.full_name, p.code AS paper, b.code AS other, b.session_name, p.day
                    FROM invigilator_assignment i
                    JOIN hall_allocation a ON a.id = i.hall_allocation_id
                    JOIN placed p ON p.schedule_id = a.exam_schedule_id
                    JOIN app_user u ON u.id = i.user_id
                    JOIN booked b ON b.slot_date = p.slot_date AND b.start_time < p.end_time AND p.start_time < b.end_time
                    JOIN hall_allocation theirs ON theirs.exam_schedule_id = b.schedule_id
                    JOIN invigilator_assignment duty ON duty.hall_allocation_id = theirs.id AND duty.user_id = i.user_id
                    WHERE i.version_id = :version
                    ORDER BY u.full_name, paper, other""",
                    (row, index) -> row.getString(1) + " is also on duty for " + row.getString(3) + " (" + row.getString(4) + ") when "
                            + row.getString(2) + " sits on " + row.getString(5) + "."));

    private final JdbcClient jdbc;

    /** One plain sentence per problem. Empty means the timetable still holds. */
    public List<String> problems(long versionId) {
        return RULES.stream()
                .flatMap(rule -> jdbc.sql(CONTEXT + rule.sql()).param("version", versionId).query(rule.message()).list().stream())
                .toList();
    }

    private static String students(int count) {
        return count + (count == 1 ? " student" : " students");
    }
}
