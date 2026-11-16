package com.slotforge.schedule.service;

import java.util.List;

import com.slotforge.audit.service.AuditService;
import com.slotforge.common.exception.ApiException;
import com.slotforge.exam.service.ApprovalAuthority;
import com.slotforge.exam.service.ExamSessionService;
import com.slotforge.notification.service.NotificationService;
import com.slotforge.schedule.validation.ScheduleValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Draft, sent for approval by a coordinator, approved by a head of department, then published by a coordinator.
 * Each step is one UPDATE guarded by the status it expects, so two people acting at once cannot both succeed.
 */
@Service
@RequiredArgsConstructor
public class ScheduleWorkflowService {

    // Any fixed number shared by every publish, so two publishes check for clashes one at a time.
    private static final long PUBLISH_LOCK = 4_001_002L;

    private record Version(long sessionId, Integer total, Integer placed, boolean unsentDraft) {
    }

    private final JdbcClient jdbc;
    private final ExamSessionService sessions;
    private final AuditService audit;
    private final ScheduleValidator validator;
    private final ApprovalAuthority authority;
    private final NotificationService notifications;

    @Transactional
    public void submit(Long actorId, Long versionId) {
        Version version = requireEditableSession(versionId);
        requireEveryPaperPlaced(version);
        ApprovalAuthority.Approver approver = authority.of(version.sessionId());
        if (approver.vacant()) {
            throw ApiException.unprocessable("NO_APPROVER", "There is no " + approver.post() + ". Ask an administrator to appoint one.");
        }
        // The newest one sent replaces whatever was under review, so an approver can never act on an abandoned version.
        jdbc.sql("""
                UPDATE schedule_version SET status = 'DRAFT', submitted_at = NULL, reviewed_by = NULL, reviewed_at = NULL
                WHERE session_id = :session AND id <> :version AND (status = 'REVIEWED' OR (status = 'DRAFT' AND submitted_at IS NOT NULL))
                RETURNING id""")
                .param("session", version.sessionId()).param("version", versionId).query(Long.class).list()
                .forEach(withdrawn -> audit.record(actorId, "SCHEDULE_WITHDRAWN", "schedule_version", withdrawn, null));
        int changed = jdbc.sql("""
                UPDATE schedule_version SET submitted_at = now(), review_comment = NULL
                WHERE id = :version AND status = 'DRAFT' AND submitted_at IS NULL""")
                .param("version", versionId).update();
        if (changed == 0) throw ApiException.conflict("ALREADY_SENT", "This timetable has already been sent for approval.");
        requireStillValid(versionId);
        audit.record(actorId, "SCHEDULE_SENT", "schedule_version", versionId, null);
        notifications.notify(approver.userIds(), "APPROVAL_REQUESTED", sessionName(version) + ": a timetable is waiting for your approval",
                null, timetableLink(version, versionId));
    }

    @Transactional
    public void approve(Long actorId, Long versionId) {
        Version version = requireEditableSession(versionId);
        requireEveryPaperPlaced(version);
        requireSent(version);
        requireApprover(actorId, version);
        int changed = jdbc.sql("""
                UPDATE schedule_version SET status = 'REVIEWED', reviewed_by = :actor, reviewed_at = now(), review_comment = NULL
                WHERE id = :version AND status = 'DRAFT' AND submitted_at IS NOT NULL""")
                .param("actor", actorId).param("version", versionId).update();
        if (changed == 0) throw ApiException.conflict("VERSION_NOT_DRAFT", "Only a draft can be approved.");
        requireStillValid(versionId);
        audit.record(actorId, "SCHEDULE_APPROVED", "schedule_version", versionId, null);
        notifications.notify(producers(versionId), "TIMETABLE_APPROVED", sessionName(version) + ": the timetable was approved and can be published",
                null, timetableLink(version, versionId));
    }

    @Transactional
    public void sendBack(Long actorId, Long versionId, String comment) {
        Version version = requireEditableSession(versionId);
        requireSent(version);
        requireApprover(actorId, version);
        int changed = jdbc.sql("""
                UPDATE schedule_version SET status = 'DRAFT', submitted_at = NULL, reviewed_by = NULL, reviewed_at = NULL, review_comment = :comment
                WHERE id = :version AND (status = 'REVIEWED' OR (status = 'DRAFT' AND submitted_at IS NOT NULL))""")
                .param("comment", comment.trim()).param("version", versionId).update();
        if (changed == 0) throw ApiException.conflict("VERSION_NOT_DRAFT", "This timetable is no longer waiting for approval.");
        audit.record(actorId, "SCHEDULE_RETURNED", "schedule_version", versionId, null);
        notifications.notify(producers(versionId), "TIMETABLE_RETURNED", sessionName(version) + ": the timetable was sent back",
                comment.trim(), timetableLink(version, versionId));
    }

    /** Replaces any earlier published timetable and locks the session, all together. */
    @Transactional
    public void publish(Long actorId, Long versionId) {
        jdbc.sql("SELECT pg_advisory_xact_lock(?)").param(PUBLISH_LOCK).query((row, index) -> true).list();
        Version version = requireEditableSession(versionId);
        jdbc.sql("UPDATE schedule_version SET status = 'SUPERSEDED' WHERE session_id = :session AND status = 'PUBLISHED'")
                .param("session", version.sessionId()).update();
        int changed = jdbc.sql("""
                UPDATE schedule_version SET status = 'PUBLISHED', published_by = :actor, published_at = now()
                WHERE id = :version AND status = 'REVIEWED'""")
                .param("actor", actorId).param("version", versionId).update();
        if (changed == 0) throw ApiException.unprocessable("VERSION_NOT_REVIEWED", "A head of department must approve this timetable first.");
        requireStillValid(versionId);
        jdbc.sql("UPDATE exam_session SET status = 'PUBLISHED', updated_at = now() WHERE id = :session")
                .param("session", version.sessionId()).update();
        audit.record(actorId, "SCHEDULE_PUBLISHED", "schedule_version", versionId, null);
        tellEveryoneInTheTimetable(sessionName(version), versionId);
    }

    // shortcut: paper, slot and enrolment edits wait on the session lock, but hall and staff edits do not, so one in this same instant is not seen. Lock those too if they become frequent.
    private void requireStillValid(Long versionId) {
        if (!validator.problems(versionId).isEmpty()) {
            throw ApiException.unprocessable("TIMETABLE_OUT_OF_DATE", "This timetable has problems that must be fixed first. Repair it, or fix the cause and generate again.");
        }
    }

    private String sessionName(Version version) {
        return jdbc.sql("SELECT name FROM exam_session WHERE id = :session").param("session", version.sessionId()).query(String.class).single();
    }

    private static String timetableLink(Version version, Long versionId) {
        return "/sessions/" + version.sessionId() + "/timetable?version=" + versionId;
    }

    /** Whoever generated the version and whoever sent it for approval: the people waiting to hear back. */
    private List<Long> producers(Long versionId) {
        return jdbc.sql("""
                SELECT created_by FROM schedule_version WHERE id = :version
                UNION
                SELECT a.actor_user_id FROM audit_log a
                WHERE a.entity_type = 'schedule_version' AND a.entity_id = CAST(:version AS VARCHAR) AND a.action = 'SCHEDULE_SENT'
                  AND a.actor_user_id IS NOT NULL""")
                .param("version", versionId).query(Long.class).list();
    }

    /** Publishing is the moment duties and seats become real, so each invigilator and each student with an account is told. */
    private void tellEveryoneInTheTimetable(String session, Long versionId) {
        jdbc.sql("SELECT user_id, count(*) FROM invigilator_assignment WHERE version_id = :version GROUP BY user_id")
                .param("version", versionId)
                .query((row, index) -> new long[] {row.getLong(1), row.getLong(2)}).list()
                .forEach(duty -> notifications.notify(List.of(duty[0]), "DUTY_ASSIGNED",
                        session + ": you have " + duty[1] + (duty[1] == 1 ? " invigilation duty" : " invigilation duties"), null, "/my/duties"));
        List<Long> students = jdbc.sql("""
                SELECT DISTINCT st.user_id
                FROM student_hall_assignment seat
                JOIN exam_schedule es ON es.id = seat.exam_schedule_id
                JOIN student st ON st.id = seat.student_id
                WHERE es.version_id = :version AND st.user_id IS NOT NULL""")
                .param("version", versionId).query(Long.class).list();
        notifications.notify(students, "TIMETABLE_PUBLISHED", session + ": your exam timetable is published", null, "/");
    }

    private static void requireEveryPaperPlaced(Version version) {
        if (version.total() != null && !version.total().equals(version.placed())) {
            throw ApiException.unprocessable("UNPLACED_PAPERS", "Every paper must be placed before this timetable can be approved.");
        }
    }

    private static void requireSent(Version version) {
        if (version.unsentDraft()) throw ApiException.unprocessable("NOT_SENT", "The coordinator has not sent this timetable for approval.");
    }

    private void requireApprover(Long actorId, Version version) {
        ApprovalAuthority.Approver approver = authority.of(version.sessionId());
        if (!approver.userIds().contains(actorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_YOUR_APPROVAL", "Only the " + approver.post() + " can act on this timetable.");
        }
    }

    private Version requireEditableSession(Long versionId) {
        Version version = jdbc.sql("""
                SELECT v.session_id, r.exams_total, r.exams_scheduled, v.status = 'DRAFT' AND v.submitted_at IS NULL
                FROM schedule_version v
                LEFT JOIN scheduling_run r ON r.id = v.source_run_id
                WHERE v.id = :version""")
                .param("version", versionId)
                .query((row, index) -> new Version(row.getLong(1), row.getObject(2, Integer.class), row.getObject(3, Integer.class), row.getBoolean(4)))
                .optional()
                .orElseThrow(() -> ApiException.notFound("Timetable version", versionId));
        sessions.requirePlanning(version.sessionId());
        return version;
    }
}
