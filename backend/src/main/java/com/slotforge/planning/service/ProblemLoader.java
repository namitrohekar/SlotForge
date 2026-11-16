package com.slotforge.planning.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slotforge.exam.entity.ExamSession;
import com.slotforge.exam.service.ExamSessionService;
import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallInfo;
import com.slotforge.planning.model.InvigilatorInfo;
import com.slotforge.planning.model.Rules;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.SlotInfo;
import com.slotforge.planning.model.Staffing;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** The only bridge from the database to the engine: one consistent read, turned into plain values. */
@Service
@RequiredArgsConstructor
public class ProblemLoader {

    private static final long NODE_BUDGET = 500_000;

    private final JdbcClient jdbc;

    /** rosters holds each exam's students in roll-number order, the order they are seated in. */
    public record Snapshot(SchedulingProblem problem, Map<Long, List<Long>> rosters, List<InvigilatorInfo> invigilators,
            int studentsPerInvigilator, int maxDuties) {
    }

    private record PaperRow(long id, String label, boolean heavy, int minutes, LocalDate earliest, LocalDate latest) {
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Snapshot load(ExamSession session) {
        Map<Long, List<Long>> rosters = rosters(session.getId());
        List<ExamNode> exams = papers(session.getId()).stream()
                .filter(paper -> rosters.containsKey(paper.id()))
                .map(paper -> new ExamNode(paper.id(), paper.label(), new HashSet<>(rosters.get(paper.id())), paper.heavy(),
                        paper.minutes(), paper.earliest(), paper.latest()))
                .toList();
        Rules rules = new Rules(session.getMaxExamsPerStudentPerDay(), session.getHeavyGapDays(), NODE_BUDGET);
        List<InvigilatorInfo> invigilators = invigilators(session.getId());
        Staffing staffing = new Staffing(invigilators, session.getStudentsPerInvigilator(), session.getMaxDutiesPerInvigilator());
        return new Snapshot(new SchedulingProblem(exams, slots(session.getId()), halls(session.getId()), rules, staffing), rosters,
                invigilators, staffing.studentsPerInvigilator(), staffing.maxDuties());
    }

    private Map<Long, List<Long>> rosters(long sessionId) {
        List<long[]> rows = jdbc.sql("""
                SELECT e.examination_id, e.student_id
                FROM examination_enrolment e
                JOIN examination x ON x.id = e.examination_id
                JOIN student s ON s.id = e.student_id
                WHERE x.session_id = :session AND x.status = 'READY' AND s.active
                ORDER BY e.examination_id, s.roll_no""")
                .param("session", sessionId)
                .query((row, index) -> new long[] {row.getLong(1), row.getLong(2)})
                .list();
        Map<Long, List<Long>> rosters = new LinkedHashMap<>();
        rows.forEach(row -> rosters.computeIfAbsent(row[0], ignored -> new ArrayList<>()).add(row[1]));
        return rosters;
    }

    private List<PaperRow> papers(long sessionId) {
        return jdbc.sql("""
                SELECT x.id, su.code || ' ' || su.name, su.is_heavy, x.duration_minutes, x.earliest_date, x.latest_date
                FROM examination x
                JOIN subject su ON su.id = x.subject_id
                WHERE x.session_id = :session AND x.status = 'READY'""")
                .param("session", sessionId)
                .query((row, index) -> new PaperRow(row.getLong(1), row.getString(2), row.getBoolean(3), row.getInt(4),
                        row.getObject(5, LocalDate.class), row.getObject(6, LocalDate.class)))
                .list();
    }

    private List<SlotInfo> slots(long sessionId) {
        // A slot that has started can no longer hold an exam, so it stays visible to the engine only as closed.
        LocalDateTime now = ExamSessionService.now();
        // Weekends are never exam days, even for a slot saved before that rule existed.
        return jdbc.sql("SELECT id, slot_date, start_time, end_time, status FROM time_slot WHERE session_id = :session AND EXTRACT(ISODOW FROM slot_date) < 6")
                .param("session", sessionId)
                .query((row, index) -> {
                    LocalDate date = row.getObject(2, LocalDate.class);
                    LocalTime start = row.getObject(3, LocalTime.class);
                    return new SlotInfo(row.getLong(1), date, start, row.getObject(4, LocalTime.class),
                            "OPEN".equals(row.getString(5)) && LocalDateTime.of(date, start).isAfter(now));
                })
                .list();
    }

    // shortcut: students busy in another published session are caught on sending, not avoided here, give ExamNode blocked slots if that becomes common.
    private List<HallInfo> halls(long sessionId) {
        Map<Long, Set<Long>> closedSlots = closedSlots(sessionId, """
                SELECT u.hall_id, t.id
                FROM hall_unavailability u
                JOIN time_slot t ON t.slot_date BETWEEN u.from_date AND u.to_date
                WHERE t.session_id = :session
                UNION
                SELECT a.hall_id, t.id
                FROM time_slot t
                JOIN time_slot other ON other.slot_date = t.slot_date AND other.session_id <> t.session_id
                     AND other.start_time < t.end_time AND t.start_time < other.end_time
                JOIN exam_session s ON s.id = other.session_id AND s.status <> 'CANCELLED'
                JOIN schedule_version v ON v.session_id = s.id AND v.status = 'PUBLISHED'
                JOIN hall_allocation a ON a.version_id = v.id AND a.time_slot_id = other.id
                WHERE t.session_id = :session""");
        return jdbc.sql("SELECT id, code, capacity FROM exam_hall WHERE status = 'AVAILABLE'")
                .query((row, index) -> new HallInfo(row.getLong(1), row.getString(2), row.getInt(3),
                        closedSlots.getOrDefault(row.getLong(1), Set.of())))
                .list();
    }

    // Staff accounts are the invigilator pool. Leave, or a duty in another published session, closes a slot.
    private List<InvigilatorInfo> invigilators(long sessionId) {
        Map<Long, Set<Long>> closedSlots = closedSlots(sessionId, """
                SELECT u.user_id, t.id
                FROM staff_unavailability u
                JOIN time_slot t ON t.slot_date BETWEEN u.from_date AND u.to_date
                WHERE t.session_id = :session
                UNION
                SELECT i.user_id, t.id
                FROM time_slot t
                JOIN time_slot other ON other.slot_date = t.slot_date AND other.session_id <> t.session_id
                     AND other.start_time < t.end_time AND t.start_time < other.end_time
                JOIN exam_session s ON s.id = other.session_id AND s.status <> 'CANCELLED'
                JOIN schedule_version v ON v.session_id = s.id AND v.status = 'PUBLISHED'
                JOIN invigilator_assignment i ON i.version_id = v.id AND i.time_slot_id = other.id
                WHERE t.session_id = :session""");
        return jdbc.sql("""
                SELECT DISTINCT u.id
                FROM app_user u JOIN user_role r ON r.user_id = u.id
                WHERE u.status = 'ACTIVE' AND r.role IN ('FACULTY', 'INVIGILATOR')
                ORDER BY u.id""")
                .query((row, index) -> new InvigilatorInfo(row.getLong(1), closedSlots.getOrDefault(row.getLong(1), Set.of())))
                .list();
    }

    /** The query returns rows of [owner id, slot id]. */
    private Map<Long, Set<Long>> closedSlots(long sessionId, String sql) {
        Map<Long, Set<Long>> closed = new HashMap<>();
        jdbc.sql(sql).param("session", sessionId)
                .query((row, index) -> closed.computeIfAbsent(row.getLong(1), ignored -> new HashSet<>()).add(row.getLong(2)))
                .list();
        return closed;
    }
}
