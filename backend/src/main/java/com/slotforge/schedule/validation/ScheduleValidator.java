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
                SELECT s.id AS session_id, s.max_exams_per_student_per_day AS max_per_day, s.heavy_gap_days AS heavy_gap
                FROM exam_session s JOIN schedule_version v ON v.session_id = s.id
                WHERE v.id = :version
            ), placed AS (
                SELECT es.id AS schedule_id, es.examination_id, su.code, su.is_heavy, x.duration_minutes, x.earliest_date, x.latest_date,
                       t.id AS slot_id, t.slot_date, t.status AS slot_status,
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
                    (row, index) -> row.getString(1) + " can no longer take " + row.getString(2) + "."));

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
