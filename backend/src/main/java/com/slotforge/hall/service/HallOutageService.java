package com.slotforge.hall.service;

import java.time.LocalDate;
import java.util.List;

import com.slotforge.audit.service.AuditService;
import com.slotforge.common.exception.ApiException;
import com.slotforge.exam.service.ExamSessionService;
import com.slotforge.hall.dto.HallOutageRequest;
import com.slotforge.hall.dto.HallOutageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dates a hall cannot be used, such as repairs or another event. The engine never places a paper there on those dates,
 * and a timetable that already did is flagged as out of date so it can be repaired.
 */
@Service
@RequiredArgsConstructor
public class HallOutageService {

    // Each running session's published timetable and its newest version: the ones an outage can spoil. Older drafts are history.
    private static final String PAPERS_AFFECTED = """
            (SELECT count(DISTINCT es.id)
             FROM hall_allocation a
             JOIN exam_schedule es ON es.id = a.exam_schedule_id
             JOIN schedule_version v ON v.id = a.version_id AND (v.status = 'PUBLISHED' OR v.version_no =
                  (SELECT max(newest.version_no) FROM schedule_version newest WHERE newest.session_id = v.session_id))
             JOIN exam_session s ON s.id = v.session_id AND s.status <> 'CANCELLED'
             JOIN time_slot t ON t.id = a.time_slot_id
             WHERE a.hall_id = :hall AND t.slot_date BETWEEN %s AND %s)""";

    private final JdbcClient jdbc;
    private final AuditService audit;
    private final HallService halls;

    @Transactional(readOnly = true)
    public List<HallOutageResponse> list(Long hallId) {
        halls.get(hallId);
        return jdbc.sql("SELECT u.id, u.from_date, u.to_date, u.reason, " + PAPERS_AFFECTED.formatted("u.from_date", "u.to_date")
                        + " FROM hall_unavailability u WHERE u.hall_id = :hall ORDER BY u.from_date, u.id")
                .param("hall", hallId)
                .query((row, index) -> new HallOutageResponse(row.getLong(1), row.getObject(2, LocalDate.class), row.getObject(3, LocalDate.class),
                        row.getString(4), row.getInt(5)))
                .list();
    }

    @Transactional
    public HallOutageResponse add(Long actorId, Long hallId, HallOutageRequest request) {
        halls.get(hallId);
        if (request.toDate().isBefore(request.fromDate())) {
            throw ApiException.unprocessable("INVALID_DATES", "The end date cannot be before the start date.");
        }
        if (request.toDate().isBefore(ExamSessionService.today())) {
            throw ApiException.unprocessable("PAST_DATE", "These dates are already past.");
        }
        if (request.fromDate().isAfter(ExamSessionService.today().plusYears(2))) {
            throw ApiException.unprocessable("INVALID_DATES", "Dates more than two years ahead cannot be recorded.");
        }
        String reason = request.reason().trim();
        long id = jdbc.sql("""
                INSERT INTO hall_unavailability (hall_id, from_date, to_date, reason, created_by)
                VALUES (:hall, :from, :to, :reason, :actor) RETURNING id""")
                .param("hall", hallId).param("from", request.fromDate()).param("to", request.toDate()).param("reason", reason).param("actor", actorId)
                .query(Long.class).single();
        audit.record(actorId, "HALL_OUTAGE_ADDED", "exam_hall", hallId, null);
        int affected = jdbc.sql("SELECT " + PAPERS_AFFECTED.formatted(":from", ":to"))
                .param("hall", hallId).param("from", request.fromDate()).param("to", request.toDate()).query(Integer.class).single();
        return new HallOutageResponse(id, request.fromDate(), request.toDate(), reason, affected);
    }

    @Transactional
    public void remove(Long actorId, Long hallId, Long outageId) {
        int removed = jdbc.sql("DELETE FROM hall_unavailability WHERE id = :id AND hall_id = :hall").param("id", outageId).param("hall", hallId).update();
        if (removed == 0) throw ApiException.notFound("Unavailable dates", outageId);
        audit.record(actorId, "HALL_OUTAGE_REMOVED", "exam_hall", hallId, null);
    }
}
