package com.slotforge.schedule.service;

import com.slotforge.audit.service.AuditService;
import com.slotforge.common.exception.ApiException;
import com.slotforge.exam.service.ExamSessionService;
import com.slotforge.schedule.validation.ScheduleValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Draft, approved by a head of department, then published by a coordinator. Each step is one UPDATE
 * guarded by the status it expects, so two people acting at once cannot both succeed.
 */
@Service
@RequiredArgsConstructor
public class ScheduleWorkflowService {

    private record Version(long sessionId, Integer total, Integer placed) {
    }

    private final JdbcClient jdbc;
    private final ExamSessionService sessions;
    private final AuditService audit;
    private final ScheduleValidator validator;

    @Transactional
    public void approve(Long actorId, Long versionId) {
        Version version = requireEditableSession(versionId);
        if (version.total() != null && !version.total().equals(version.placed())) {
            throw ApiException.unprocessable("UNPLACED_PAPERS", "Every paper must be placed before this timetable can be approved.");
        }
        int changed = jdbc.sql("""
                UPDATE schedule_version SET status = 'REVIEWED', reviewed_by = :actor, reviewed_at = now(), review_comment = NULL
                WHERE id = :version AND status = 'DRAFT'""")
                .param("actor", actorId).param("version", versionId).update();
        if (changed == 0) throw ApiException.conflict("VERSION_NOT_DRAFT", "Only a draft can be approved.");
        requireStillValid(versionId);
        audit.record(actorId, "SCHEDULE_APPROVED", "schedule_version", versionId, null);
    }

    @Transactional
    public void sendBack(Long actorId, Long versionId, String comment) {
        requireEditableSession(versionId);
        int changed = jdbc.sql("""
                UPDATE schedule_version SET status = 'DRAFT', reviewed_by = NULL, reviewed_at = NULL, review_comment = :comment
                WHERE id = :version AND status IN ('DRAFT', 'REVIEWED')""")
                .param("comment", comment.trim()).param("version", versionId).update();
        if (changed == 0) throw ApiException.conflict("VERSION_NOT_DRAFT", "A published timetable cannot be sent back.");
        audit.record(actorId, "SCHEDULE_RETURNED", "schedule_version", versionId, null);
    }

    /** Replaces any earlier published timetable and locks the session, all together. */
    @Transactional
    public void publish(Long actorId, Long versionId) {
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
    }

    // shortcut: an edit committed in the instant after this check is not seen, lock the session row in every input writer if edits ever become concurrent.
    private void requireStillValid(Long versionId) {
        if (!validator.problems(versionId).isEmpty()) {
            throw ApiException.unprocessable("TIMETABLE_OUT_OF_DATE", "This timetable no longer matches its papers, slots or halls. It has to be generated again.");
        }
    }

    private Version requireEditableSession(Long versionId) {
        Version version = jdbc.sql("""
                SELECT v.session_id, r.exams_total, r.exams_scheduled
                FROM schedule_version v
                LEFT JOIN scheduling_run r ON r.id = v.source_run_id
                WHERE v.id = :version""")
                .param("version", versionId)
                .query((row, index) -> new Version(row.getLong(1), row.getObject(2, Integer.class), row.getObject(3, Integer.class)))
                .optional()
                .orElseThrow(() -> ApiException.notFound("Timetable version", versionId));
        sessions.requirePlanning(version.sessionId());
        return version;
    }
}
