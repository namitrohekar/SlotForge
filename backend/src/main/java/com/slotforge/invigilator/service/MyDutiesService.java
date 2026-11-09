package com.slotforge.invigilator.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import com.slotforge.audit.service.AuditService;
import com.slotforge.common.exception.ApiException;
import com.slotforge.invigilator.dto.DutyResponse;
import com.slotforge.invigilator.dto.UnavailabilityRequest;
import com.slotforge.invigilator.dto.UnavailabilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Everything here is scoped to the signed-in person, so nobody can reach another's duties or leave by changing an id. */
@Service
@RequiredArgsConstructor
public class MyDutiesService {

    private static final ZoneId INSTITUTE_ZONE = ZoneId.of("Asia/Kolkata");

    private static final String PUBLISHED_DUTIES_IN_RANGE = """
            (SELECT count(*)
             FROM invigilator_assignment i
             JOIN schedule_version v ON v.id = i.version_id AND v.status = 'PUBLISHED'
             JOIN exam_session s ON s.id = v.session_id AND s.status <> 'CANCELLED'
             JOIN time_slot t ON t.id = i.time_slot_id
             WHERE i.user_id = :user AND t.slot_date BETWEEN %s AND %s)""";

    private final JdbcClient jdbc;
    private final AuditService audit;

    /** Published timetables only: a draft may still change, so nobody is told about it yet. */
    @Transactional(readOnly = true)
    public List<DutyResponse> duties(Long userId) {
        return jdbc.sql("""
                SELECT t.slot_date, t.start_time, t.end_time, h.code, h.name, h.building, su.code, su.name, a.seats_allocated, s.name,
                       EXISTS (SELECT 1 FROM staff_unavailability l WHERE l.user_id = i.user_id AND t.slot_date BETWEEN l.from_date AND l.to_date)
                FROM invigilator_assignment i
                JOIN schedule_version v ON v.id = i.version_id AND v.status = 'PUBLISHED'
                JOIN exam_session s ON s.id = v.session_id AND s.status <> 'CANCELLED'
                JOIN hall_allocation a ON a.id = i.hall_allocation_id
                JOIN exam_hall h ON h.id = a.hall_id
                JOIN exam_schedule es ON es.id = a.exam_schedule_id
                JOIN examination x ON x.id = es.examination_id
                JOIN subject su ON su.id = x.subject_id
                JOIN time_slot t ON t.id = i.time_slot_id
                WHERE i.user_id = :user
                ORDER BY t.slot_date, t.start_time, h.code""")
                .param("user", userId)
                .query((row, index) -> new DutyResponse(row.getObject(1, LocalDate.class), row.getObject(2, LocalTime.class),
                        row.getObject(3, LocalTime.class), row.getString(4), row.getString(5), row.getString(6), row.getString(7),
                        row.getString(8), row.getInt(9), row.getString(10), row.getBoolean(11)))
                .list();
    }

    @Transactional(readOnly = true)
    public List<UnavailabilityResponse> unavailability(Long userId) {
        return jdbc.sql("SELECT l.id, l.from_date, l.to_date, l.reason, " + PUBLISHED_DUTIES_IN_RANGE.formatted("l.from_date", "l.to_date")
                        + " FROM staff_unavailability l WHERE l.user_id = :user ORDER BY l.from_date, l.id")
                .param("user", userId)
                .query((row, index) -> new UnavailabilityResponse(row.getLong(1), row.getObject(2, LocalDate.class),
                        row.getObject(3, LocalDate.class), row.getString(4), row.getInt(5)))
                .list();
    }

    @Transactional
    public UnavailabilityResponse add(Long userId, UnavailabilityRequest request) {
        if (request.toDate().isBefore(request.fromDate())) {
            throw ApiException.unprocessable("INVALID_DATES", "The end date cannot be before the start date.");
        }
        if (request.fromDate().isBefore(LocalDate.now(INSTITUTE_ZONE))) {
            throw ApiException.unprocessable("PAST_DATE", "Unavailable dates cannot be in the past.");
        }
        String reason = request.reason().trim();
        long id = jdbc.sql("""
                INSERT INTO staff_unavailability (user_id, from_date, to_date, reason, created_by)
                VALUES (:user, :from, :to, :reason, :user) RETURNING id""")
                .param("user", userId).param("from", request.fromDate()).param("to", request.toDate()).param("reason", reason)
                .query(Long.class).single();
        audit.record(userId, "UNAVAILABILITY_ADDED", "staff_unavailability", id, null);
        int affected = jdbc.sql("SELECT " + PUBLISHED_DUTIES_IN_RANGE.formatted(":from", ":to"))
                .param("user", userId).param("from", request.fromDate()).param("to", request.toDate()).query(Integer.class).single();
        return new UnavailabilityResponse(id, request.fromDate(), request.toDate(), reason, affected);
    }

    @Transactional
    public void remove(Long userId, Long id) {
        int removed = jdbc.sql("DELETE FROM staff_unavailability WHERE id = :id AND user_id = :user")
                .param("id", id).param("user", userId).update();
        if (removed == 0) throw ApiException.notFound("Unavailable dates", id);
        audit.record(userId, "UNAVAILABILITY_REMOVED", "staff_unavailability", id, null);
    }
}
