package com.slotforge.planning.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slotforge.exam.entity.ExamSession;
import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallInfo;
import com.slotforge.planning.model.Rules;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.SlotInfo;
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
    public record Snapshot(SchedulingProblem problem, Map<Long, List<Long>> rosters) {
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
        return new Snapshot(new SchedulingProblem(exams, slots(session.getId()), halls(session.getId()), rules), rosters);
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
        // Weekends are never exam days, even for a slot saved before that rule existed.
        return jdbc.sql("SELECT id, slot_date, start_time, end_time, status FROM time_slot WHERE session_id = :session AND EXTRACT(ISODOW FROM slot_date) < 6")
                .param("session", sessionId)
                .query((row, index) -> new SlotInfo(row.getLong(1), row.getObject(2, LocalDate.class),
                        row.getObject(3, LocalTime.class), row.getObject(4, LocalTime.class), "OPEN".equals(row.getString(5))))
                .list();
    }

    private List<HallInfo> halls(long sessionId) {
        List<long[]> outages = jdbc.sql("""
                SELECT u.hall_id, t.id
                FROM hall_unavailability u
                JOIN time_slot t ON t.slot_date BETWEEN u.from_date AND u.to_date
                WHERE t.session_id = :session""")
                .param("session", sessionId)
                .query((row, index) -> new long[] {row.getLong(1), row.getLong(2)})
                .list();
        Map<Long, Set<Long>> closedSlots = new HashMap<>();
        outages.forEach(row -> closedSlots.computeIfAbsent(row[0], ignored -> new HashSet<>()).add(row[1]));
        return jdbc.sql("SELECT id, code, capacity FROM exam_hall WHERE status = 'AVAILABLE'")
                .query((row, index) -> new HallInfo(row.getLong(1), row.getString(2), row.getInt(3),
                        closedSlots.getOrDefault(row.getLong(1), Set.of())))
                .list();
    }
}
