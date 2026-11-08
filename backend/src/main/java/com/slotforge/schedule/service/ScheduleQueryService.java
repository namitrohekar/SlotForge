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
import com.slotforge.exam.service.ExamSessionService;
import com.slotforge.planning.dto.RunReport;
import com.slotforge.planning.model.Unplaced;
import com.slotforge.schedule.dto.AwaitingApprovalResponse;
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

    private record VersionKey(long sessionId, String status) {
    }

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final ExamSessionService sessions;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<VersionResponse> versions(Long sessionId) {
        sessions.get(sessionId);
        return jdbc.sql("""
                SELECT v.id, v.version_no, v.status, v.created_at, r.exams_total, r.exams_scheduled, r.slots_used,
                       r.search_nodes, r.backtracks, r.duration_ms, r.report, v.review_comment, reviewer.full_name,
                       v.reviewed_at, v.published_at
                FROM schedule_version v
                LEFT JOIN scheduling_run r ON r.id = v.source_run_id
                LEFT JOIN app_user reviewer ON reviewer.id = v.reviewed_by
                WHERE v.session_id = :session
                ORDER BY v.version_no DESC""")
                .param("session", sessionId)
                .query((row, index) -> new VersionResponse(row.getLong(1), row.getInt(2), row.getString(3),
                        row.getObject(4, OffsetDateTime.class), row.getObject(5, Integer.class), row.getObject(6, Integer.class),
                        row.getObject(7, Integer.class), row.getObject(8, Long.class), row.getObject(9, Long.class),
                        row.getObject(10, Long.class), unplaced(row.getString(11)), row.getString(12), row.getString(13),
                        row.getObject(14, OffsetDateTime.class), row.getObject(15, OffsetDateTime.class)))
                .list();
    }

    /** The newest complete, un-returned draft of each session being planned: what a reviewer has to act on. */
    @Transactional(readOnly = true)
    public List<AwaitingApprovalResponse> awaitingApproval() {
        return jdbc.sql("""
                SELECT DISTINCT ON (s.id) v.id, v.version_no, s.id, s.name, r.exams_total, v.created_at
                FROM schedule_version v
                JOIN exam_session s ON s.id = v.session_id AND s.status = 'PLANNING'
                JOIN scheduling_run r ON r.id = v.source_run_id AND r.exams_scheduled = r.exams_total
                WHERE v.status = 'DRAFT' AND v.review_comment IS NULL
                ORDER BY s.id, v.version_no DESC""")
                .query((row, index) -> new AwaitingApprovalResponse(row.getLong(1), row.getInt(2), row.getLong(3), row.getString(4),
                        row.getInt(5), row.getObject(6, OffsetDateTime.class)))
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
        Map<Long, List<ScheduleEntryResponse.Hall>> byEntry = new HashMap<>();
        jdbc.sql("""
                SELECT a.exam_schedule_id, h.code, h.name, h.building, a.seats_allocated, a.capacity_snapshot
                FROM hall_allocation a
                JOIN exam_hall h ON h.id = a.hall_id
                WHERE a.version_id = :version
                ORDER BY a.seats_allocated DESC, h.code""")
                .param("version", versionId)
                .query((row, index) -> {
                    byEntry.computeIfAbsent(row.getLong(1), ignored -> new ArrayList<>()).add(new ScheduleEntryResponse.Hall(
                            row.getString(2), row.getString(3), row.getString(4), row.getInt(5), row.getInt(6)));
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
        jdbc.sql("DELETE FROM schedule_version WHERE id = :version").param("version", versionId).update();
        audit.record(actorId, "SCHEDULE_DELETED", "schedule_version", versionId, null);
    }

    private VersionKey find(Long versionId) {
        return jdbc.sql("SELECT session_id, status FROM schedule_version WHERE id = :version")
                .param("version", versionId)
                .query((row, index) -> new VersionKey(row.getLong(1), row.getString(2)))
                .optional()
                .orElseThrow(() -> ApiException.notFound("Timetable version", versionId));
    }
}
