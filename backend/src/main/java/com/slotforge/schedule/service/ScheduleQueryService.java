package com.slotforge.schedule.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.slotforge.audit.service.AuditService;
import com.slotforge.common.exception.ApiException;
import com.slotforge.exam.service.ApprovalAuthority;
import com.slotforge.exam.service.ExamSessionService;
import com.slotforge.planning.dto.RunReport;
import com.slotforge.planning.model.Unplaced;
import com.slotforge.schedule.dto.AwaitingApprovalResponse;
import com.slotforge.schedule.dto.DutyAtRiskResponse;
import com.slotforge.schedule.dto.ScheduleEntryResponse;
import com.slotforge.schedule.dto.VersionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class ScheduleQueryService {

    private record VersionKey(long sessionId, String status, boolean sent) {
    }

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final ExamSessionService sessions;
    private final AuditService audit;
    private final ApprovalAuthority authority;

    @Transactional(readOnly = true)
    public List<VersionResponse> versions(Long sessionId) {
        sessions.get(sessionId);
        return jdbc.sql("""
                SELECT v.id, v.version_no, v.status, v.created_at, r.exams_total, r.exams_scheduled, r.slots_used,
                       r.search_nodes, r.backtracks, r.duration_ms, r.report, v.review_comment, reviewer.full_name,
                       v.reviewed_at, v.published_at, v.submitted_at, generator.full_name,
                       (SELECT sender.full_name FROM audit_log a JOIN app_user sender ON sender.id = a.actor_user_id
                        WHERE a.entity_type = 'schedule_version' AND a.entity_id = CAST(v.id AS VARCHAR) AND a.action = 'SCHEDULE_SENT'
                        ORDER BY a.created_at DESC LIMIT 1),
                       publisher.full_name,
                       (SELECT count(DISTINCT i.user_id) FROM invigilator_assignment i WHERE i.version_id = v.id)
                FROM schedule_version v
                JOIN app_user generator ON generator.id = v.created_by
                LEFT JOIN app_user publisher ON publisher.id = v.published_by
                LEFT JOIN scheduling_run r ON r.id = v.source_run_id
                LEFT JOIN app_user reviewer ON reviewer.id = v.reviewed_by
                WHERE v.session_id = :session
                ORDER BY v.version_no DESC""")
                .param("session", sessionId)
                .query((row, index) -> new VersionResponse(row.getLong(1), row.getInt(2), row.getString(3),
                        row.getObject(4, OffsetDateTime.class), row.getObject(5, Integer.class), row.getObject(6, Integer.class),
                        row.getObject(7, Integer.class), row.getObject(8, Long.class), row.getObject(9, Long.class),
                        row.getObject(10, Long.class), unplaced(row.getString(11)), row.getString(12), row.getString(13),
                        row.getObject(14, OffsetDateTime.class), row.getObject(15, OffsetDateTime.class), row.getObject(16, OffsetDateTime.class),
                        row.getString(17), row.getString(18), row.getString(19), row.getInt(20)))
                .list();
    }

    /** The drafts sent for approval that this person, and nobody else, has to act on. */
    @Transactional(readOnly = true)
    public List<AwaitingApprovalResponse> awaitingApproval(Long actorId) {
        return jdbc.sql("""
                SELECT v.id, v.version_no, s.id, s.name, r.exams_total, v.created_at
                FROM schedule_version v
                JOIN exam_session s ON s.id = v.session_id AND s.status = 'PLANNING'
                JOIN scheduling_run r ON r.id = v.source_run_id
                WHERE v.status = 'DRAFT' AND v.submitted_at IS NOT NULL
                ORDER BY v.created_at""")
                .query((row, index) -> new AwaitingApprovalResponse(row.getLong(1), row.getInt(2), row.getLong(3), row.getString(4),
                        row.getInt(5), row.getObject(6, OffsetDateTime.class)))
                .list().stream()
                .filter(waiting -> authority.of(waiting.sessionId()).userIds().contains(actorId))
                .toList();
    }

    /** Published, upcoming duties whose invigilator can no longer do them. */
    @Transactional(readOnly = true)
    public List<DutyAtRiskResponse> dutiesAtRisk() {
        return jdbc.sql("""
                SELECT s.id, s.name, u.full_name, su.code, h.code, t.slot_date
                FROM invigilator_assignment i
                JOIN schedule_version v ON v.id = i.version_id AND v.status = 'PUBLISHED'
                JOIN exam_session s ON s.id = v.session_id AND s.status <> 'CANCELLED'
                JOIN app_user u ON u.id = i.user_id
                JOIN hall_allocation a ON a.id = i.hall_allocation_id
                JOIN exam_hall h ON h.id = a.hall_id
                JOIN exam_schedule es ON es.id = a.exam_schedule_id
                JOIN examination x ON x.id = es.examination_id
                JOIN subject su ON su.id = x.subject_id
                JOIN time_slot t ON t.id = i.time_slot_id
                WHERE t.slot_date >= :today
                  AND (u.status <> 'ACTIVE'
                       OR NOT EXISTS (SELECT 1 FROM user_role r WHERE r.user_id = u.id AND r.role IN ('FACULTY', 'INVIGILATOR'))
                       OR EXISTS (SELECT 1 FROM staff_unavailability l WHERE l.user_id = u.id AND t.slot_date BETWEEN l.from_date AND l.to_date))
                ORDER BY t.slot_date, s.name, su.code""")
                .param("today", ExamSessionService.today())
                .query((row, index) -> new DutyAtRiskResponse(row.getLong(1), row.getString(2), row.getString(3), row.getString(4),
                        row.getString(5), row.getObject(6, LocalDate.class)))
                .list();
    }

    private List<Unplaced> unplaced(String report) {
        return report == null ? List.of() : json.readValue(report, RunReport.class).unplaced();
    }

    @Transactional(readOnly = true)
    public List<ScheduleEntryResponse> entries(Long versionId) {
        find(versionId);
        Map<Long, List<ScheduleEntryResponse.Hall>> halls = halls(versionId);
        Map<Long, List<ScheduleEntryResponse.Conflict>> conflicts = conflicts(versionId);
        return jdbc.sql("""
                SELECT e.examination_id, su.code, su.name, su.is_heavy, d.code, x.duration_minutes,
                       t.id, t.slot_date, t.start_time, t.end_time, e.id
                FROM exam_schedule e
                JOIN examination x ON x.id = e.examination_id
                JOIN subject su ON su.id = x.subject_id
                JOIN department d ON d.id = su.department_id
                JOIN time_slot t ON t.id = e.time_slot_id
                WHERE e.version_id = :version
                ORDER BY t.slot_date, t.start_time, su.code""")
                .param("version", versionId)
                .query((row, index) -> {
                    List<ScheduleEntryResponse.Hall> rooms = halls.getOrDefault(row.getLong(11), List.of());
                    return new ScheduleEntryResponse(row.getLong(1), row.getString(2), row.getString(3), row.getBoolean(4),
                            row.getString(5), row.getInt(6), row.getLong(7), row.getObject(8, LocalDate.class),
                            row.getObject(9, LocalTime.class), row.getObject(10, LocalTime.class),
                            rooms.stream().mapToInt(ScheduleEntryResponse.Hall::seats).sum(), rooms,
                            conflicts.getOrDefault(row.getLong(1), List.of()));
                })
                .list();
    }

    private Map<Long, List<ScheduleEntryResponse.Hall>> halls(long versionId) {
        Map<Long, List<String>> invigilators = new HashMap<>();
        jdbc.sql("""
                SELECT i.hall_allocation_id, u.full_name
                FROM invigilator_assignment i JOIN app_user u ON u.id = i.user_id
                WHERE i.version_id = :version
                ORDER BY u.full_name""")
                .param("version", versionId)
                .query((row, index) -> invigilators.computeIfAbsent(row.getLong(1), ignored -> new ArrayList<>()).add(row.getString(2)))
                .list();
        Map<Long, List<ScheduleEntryResponse.Hall>> byEntry = new HashMap<>();
        jdbc.sql("""
                SELECT a.exam_schedule_id, h.code, h.name, h.building, a.seats_allocated, a.capacity_snapshot, a.id
                FROM hall_allocation a
                JOIN exam_hall h ON h.id = a.hall_id
                WHERE a.version_id = :version
                ORDER BY a.seats_allocated DESC, h.code""")
                .param("version", versionId)
                .query((row, index) -> {
                    byEntry.computeIfAbsent(row.getLong(1), ignored -> new ArrayList<>()).add(new ScheduleEntryResponse.Hall(
                            row.getString(2), row.getString(3), row.getString(4), row.getInt(5), row.getInt(6),
                            invigilators.getOrDefault(row.getLong(7), List.of())));
                    return true;
                })
                .list();
        return byEntry;
    }

    private Map<Long, List<ScheduleEntryResponse.Conflict>> conflicts(long versionId) {
        Map<Long, List<ScheduleEntryResponse.Conflict>> byExam = new HashMap<>();
        jdbc.sql("""
                SELECT a.examination_id, b.examination_id, count(*)
                FROM examination_enrolment a
                JOIN examination_enrolment b ON b.student_id = a.student_id AND a.examination_id < b.examination_id
                JOIN exam_schedule sa ON sa.examination_id = a.examination_id AND sa.version_id = :version
                JOIN exam_schedule sb ON sb.examination_id = b.examination_id AND sb.version_id = :version
                GROUP BY a.examination_id, b.examination_id""")
                .param("version", versionId)
                .query((row, index) -> {
                    long first = row.getLong(1);
                    long second = row.getLong(2);
                    int shared = row.getInt(3);
                    byExam.computeIfAbsent(first, ignored -> new ArrayList<>()).add(new ScheduleEntryResponse.Conflict(second, shared));
                    byExam.computeIfAbsent(second, ignored -> new ArrayList<>()).add(new ScheduleEntryResponse.Conflict(first, shared));
                    return true;
                })
                .list();
        return byExam;
    }

    @Transactional
    public void deleteDraft(Long actorId, Long versionId) {
        VersionKey version = find(versionId);
        sessions.requirePlanning(version.sessionId());
        if (!"DRAFT".equals(version.status())) {
            throw ApiException.conflict("VERSION_NOT_DRAFT", "Only a draft can be deleted.");
        }
        if (version.sent()) {
            throw ApiException.conflict("VERSION_IN_REVIEW", "This version is with the approver. Send a newer version to replace it.");
        }
        jdbc.sql("DELETE FROM schedule_version WHERE id = :version").param("version", versionId).update();
        audit.record(actorId, "SCHEDULE_DELETED", "schedule_version", versionId, null);
    }

    private VersionKey find(Long versionId) {
        return jdbc.sql("SELECT session_id, status, submitted_at IS NOT NULL FROM schedule_version WHERE id = :version")
                .param("version", versionId)
                .query((row, index) -> new VersionKey(row.getLong(1), row.getString(2), row.getBoolean(3)))
                .optional()
                .orElseThrow(() -> ApiException.notFound("Timetable version", versionId));
    }
}
