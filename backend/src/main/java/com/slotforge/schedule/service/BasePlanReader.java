package com.slotforge.schedule.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.slotforge.common.exception.ApiException;
import com.slotforge.planning.model.HallSeats;
import com.slotforge.planning.repair.RepairPlanner.Base;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads a stored timetable in the shape a new run can build on. */
@Service
@RequiredArgsConstructor
public class BasePlanReader {

    /** invigilators is keyed by paper and hall, the two things that stay the same between versions. */
    public record BasePlan(
            long versionId, long sessionId, String status, boolean sent, Long parentVersionId, Base base, Set<Long> locked,
            Map<String, List<Long>> invigilators) {

        public Map<Long, Long> lockedSlots() {
            Map<Long, Long> slots = new HashMap<>();
            locked.forEach(exam -> slots.put(exam, base.slotByExam().get(exam)));
            return slots;
        }

        public boolean editable() {
            return "DRAFT".equals(status) && !sent;
        }
    }

    private final JdbcClient jdbc;

    public static String key(long examId, long hallId) {
        return examId + ":" + hallId;
    }

    @Transactional(readOnly = true)
    public BasePlan read(Long versionId) {
        record Head(long sessionId, String status, boolean sent, Long parent) {
        }
        Head head = jdbc.sql("SELECT session_id, status, submitted_at IS NOT NULL, parent_version_id FROM schedule_version WHERE id = :version")
                .param("version", versionId)
                .query((row, index) -> new Head(row.getLong(1), row.getString(2), row.getBoolean(3), row.getObject(4, Long.class)))
                .optional().orElseThrow(() -> ApiException.notFound("Timetable version", versionId));

        Map<Long, Long> slots = new HashMap<>();
        Set<Long> locked = new HashSet<>();
        jdbc.sql("SELECT examination_id, time_slot_id, locked FROM exam_schedule WHERE version_id = :version")
                .param("version", versionId)
                .query((row, index) -> {
                    slots.put(row.getLong(1), row.getLong(2));
                    if (row.getBoolean(3)) locked.add(row.getLong(1));
                    return index;
                })
                .list();

        Map<Long, List<HallSeats>> halls = new HashMap<>();
        jdbc.sql("""
                SELECT es.examination_id, a.hall_id, a.seats_allocated
                FROM hall_allocation a JOIN exam_schedule es ON es.id = a.exam_schedule_id
                WHERE a.version_id = :version
                ORDER BY a.id""")
                .param("version", versionId)
                .query((row, index) -> halls.computeIfAbsent(row.getLong(1), ignored -> new ArrayList<>()).add(new HallSeats(row.getLong(2), row.getInt(3))))
                .list();

        Map<String, List<Long>> invigilators = new HashMap<>();
        jdbc.sql("""
                SELECT es.examination_id, a.hall_id, i.user_id
                FROM invigilator_assignment i
                JOIN hall_allocation a ON a.id = i.hall_allocation_id
                JOIN exam_schedule es ON es.id = a.exam_schedule_id
                WHERE i.version_id = :version
                ORDER BY i.id""")
                .param("version", versionId)
                .query((row, index) -> invigilators.computeIfAbsent(key(row.getLong(1), row.getLong(2)), ignored -> new ArrayList<>()).add(row.getLong(3)))
                .list();

        return new BasePlan(versionId, head.sessionId(), head.status(), head.sent(), head.parent(), new Base(slots, halls), locked, invigilators);
    }

    /** The newest version of a session, whose locked papers a fresh run keeps. */
    @Transactional(readOnly = true)
    public Optional<BasePlan> latest(Long sessionId) {
        return jdbc.sql("SELECT id FROM schedule_version WHERE session_id = :session ORDER BY version_no DESC LIMIT 1")
                .param("session", sessionId).query(Long.class).optional().map(this::read);
    }
}
