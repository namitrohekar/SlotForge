package com.slotforge.exam.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.slotforge.audit.service.AuditService;
import com.slotforge.common.exception.ApiException;
import com.slotforge.common.persistence.QuerySupport;
import com.slotforge.common.response.PageResponse;
import com.slotforge.exam.dto.ExamSessionRequest;
import com.slotforge.exam.dto.ExamSessionResponse;
import com.slotforge.exam.dto.SessionProgress;
import com.slotforge.exam.entity.ExamSession;
import com.slotforge.exam.entity.SessionStatus;
import com.slotforge.exam.repository.ExamSessionRepository;
import com.slotforge.exam.repository.TimeSlotRepository;
import com.slotforge.schedule.validation.ScheduleValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ExamSessionService {

    private static final Set<String> SORTABLE = Set.of("name", "startDate");
    private static final int MAX_SESSION_DAYS = 120;
    private static final ZoneId INSTITUTE_ZONE = ZoneId.of("Asia/Kolkata");

    private final ExamSessionRepository sessions;
    private final TimeSlotRepository slots;
    private final AuditService audit;
    private final JdbcClient jdbc;
    private final ApprovalAuthority authority;
    private final ScheduleValidator validator;

    @Transactional(readOnly = true)
    public PageResponse<ExamSessionResponse> search(String q, Pageable pageable) {
        QuerySupport.requireSortable(pageable, SORTABLE);
        Specification<ExamSession> spec = (root, query, cb) -> StringUtils.hasText(q)
                ? cb.like(cb.lower(root.get("name")), QuerySupport.containsPattern(q), '\\')
                : cb.conjunction();
        var page = sessions.findAll(spec, pageable);
        List<Long> ids = page.getContent().stream().map(ExamSession::getId).toList();
        Map<Long, Long> slotCounts = QuerySupport.counts(ids, sessions::slotCounts);
        Map<Long, Long> examCounts = QuerySupport.counts(ids, sessions::examinationCounts);
        return PageResponse.from(page.map(s -> ExamSessionResponse.from(s,
                slotCounts.getOrDefault(s.getId(), 0L), examCounts.getOrDefault(s.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public ExamSessionResponse get(Long id) {
        ExamSession session = find(id);
        List<Long> ids = List.of(id);
        return ExamSessionResponse.from(session, QuerySupport.counts(ids, sessions::slotCounts).getOrDefault(id, 0L),
                QuerySupport.counts(ids, sessions::examinationCounts).getOrDefault(id, 0L), progress(id));
    }

    /** Everything the guided path needs to say where a session stands and what is in the way. */
    private SessionProgress progress(long id) {
        record Papers(int ready, Integer shortest) {
        }
        record Latest(long id, int number, String status, Integer placed, Integer total) {
        }
        Papers papers = jdbc.sql("SELECT count(*), min(duration_minutes) FROM examination WHERE session_id = :session AND status = 'READY'")
                .param("session", id).query((row, index) -> new Papers(row.getInt(1), row.getObject(2, Integer.class))).single();
        List<String> withoutStudents = jdbc.sql("""
                SELECT su.code || ' ' || su.name FROM examination x JOIN subject su ON su.id = x.subject_id
                WHERE x.session_id = :session AND x.status = 'DRAFT' ORDER BY su.code""")
                .param("session", id).query(String.class).list();
        int openSlots = jdbc.sql("SELECT count(*) FROM time_slot WHERE session_id = :session AND status = 'OPEN' AND EXTRACT(ISODOW FROM slot_date) < 6 AND slot_date + start_time > :now")
                .param("session", id).param("now", now()).query(Integer.class).single();
        int tooShort = papers.shortest() == null ? 0 : jdbc.sql("""
                SELECT count(*) FROM time_slot WHERE session_id = :session AND status = 'OPEN' AND EXTRACT(ISODOW FROM slot_date) < 6
                  AND slot_date + start_time > :now
                  AND EXTRACT(EPOCH FROM (end_time - start_time)) / 60 < :minutes""")
                .param("session", id).param("now", now()).param("minutes", papers.shortest()).query(Integer.class).single();
        Latest latest = jdbc.sql("""
                SELECT v.id, v.version_no, v.status, r.exams_scheduled, r.exams_total
                FROM schedule_version v LEFT JOIN scheduling_run r ON r.id = v.source_run_id
                WHERE v.session_id = :session ORDER BY v.version_no DESC LIMIT 1""")
                .param("session", id)
                .query((row, index) -> new Latest(row.getLong(1), row.getInt(2), row.getString(3), row.getObject(4, Integer.class), row.getObject(5, Integer.class)))
                .optional().orElse(new Latest(0, 0, null, null, null));
        String inReview = jdbc.sql("SELECT status FROM schedule_version WHERE session_id = :session AND (status = 'REVIEWED' OR (status = 'DRAFT' AND submitted_at IS NOT NULL))")
                .param("session", id).query(String.class).optional().orElse(null);
        // Only a complete draft nobody has sent can be called ready, so only that one needs checking.
        boolean sendable = "DRAFT".equals(latest.status()) && inReview == null && Objects.equals(latest.placed(), latest.total());
        int latestProblems = sendable ? validator.problems(latest.id()).size() : 0;
        ApprovalAuthority.Approver approver = authority.of(id);
        return new SessionProgress(openSlots, tooShort, papers.shortest(), papers.ready(), withoutStudents, latest.number(), latest.status(),
                latest.placed(), latest.total(), inReview, latestProblems, approver.post(), approver.names(), approver.userIds());
    }

    @Transactional
    public ExamSessionResponse create(Long actorId, ExamSessionRequest request) {
        ExamSession session = new ExamSession();
        apply(session, request);
        sessions.save(session);
        audit.record(actorId, "SESSION_CREATED", "exam_session", session.getId(), null);
        return ExamSessionResponse.from(session, 0, 0);
    }

    @Transactional
    public ExamSessionResponse update(Long actorId, Long id, ExamSessionRequest request) {
        ExamSession session = requirePlanning(id);
        boolean strandsASlot = slots.findBySessionIdOrderBySlotDateAscStartTimeAsc(id).stream()
                .anyMatch(slot -> slot.getSlotDate().isBefore(request.startDate()) || slot.getSlotDate().isAfter(request.endDate()));
        if (strandsASlot) {
            throw ApiException.unprocessable("SLOTS_OUTSIDE_DATES", "Some time slots fall outside these dates. Delete them first.");
        }
        apply(session, request);
        audit.record(actorId, "SESSION_UPDATED", "exam_session", id, null);
        return withCounts(session);
    }

    /** Removes a session being planned and everything inside it. Anything past draft must be kept, so it blocks this. */
    @Transactional
    public void delete(Long actorId, Long id) {
        ExamSession session = requirePlanning(id);
        long pastDraft = jdbc.sql("SELECT count(*) FROM schedule_version WHERE session_id = :session AND status <> 'DRAFT'")
                .param("session", id).query(Long.class).single();
        if (pastDraft > 0) {
            throw ApiException.conflict("IN_USE", session.getName() + " has a reviewed timetable, so it cannot be deleted. Cancel it instead.");
        }
        // Order matters: drafts hold references to papers and slots.
        jdbc.sql("DELETE FROM schedule_version WHERE session_id = :session").param("session", id).update();
        jdbc.sql("DELETE FROM examination WHERE session_id = :session").param("session", id).update();
        sessions.delete(session);
        sessions.flush();
        audit.record(actorId, "SESSION_DELETED", "exam_session", id, null);
    }

    /** Cancelling keeps the whole record but freezes it. Reopening is the only way back. */
    @Transactional
    public ExamSessionResponse changeStatus(Long actorId, Long id, SessionStatus target) {
        lockRow(id);
        ExamSession session = find(id);
        boolean cancelling = session.getStatus() == SessionStatus.PLANNING && target == SessionStatus.CANCELLED;
        // A reopened published session keeps its timetable live until a new one is published.
        boolean reopening = target == SessionStatus.PLANNING
                && (session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.PUBLISHED);
        if (!cancelling && !reopening) {
            throw ApiException.unprocessable("INVALID_TRANSITION", "A " + session.getStatus().name().toLowerCase(Locale.ROOT) + " session cannot become " + target.name().toLowerCase(Locale.ROOT) + ".");
        }
        session.setStatus(target);
        audit.record(actorId, cancelling ? "SESSION_CANCELLED" : "SESSION_REOPENED", "exam_session", id, null);
        return withCounts(session);
    }

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= DayOfWeek.SATURDAY.getValue();
    }

    /** This moment at the institute, whatever zone the server runs in. */
    public static LocalDateTime now() {
        return LocalDateTime.now(INSTITUTE_ZONE);
    }

    /** Today at the institute, whatever zone the server runs in. */
    public static LocalDate today() {
        return LocalDate.now(INSTITUTE_ZONE);
    }

    /** Timetabling inputs change only while a session is being planned, so stored history stays true. */
    public ExamSession requirePlanning(Long id) {
        lockRow(id);
        ExamSession session = find(id);
        if (session.getStatus() != SessionStatus.PLANNING) {
            throw ApiException.conflict("SESSION_LOCKED", session.getName() + " is no longer being planned, so it cannot be changed.");
        }
        return session;
    }

    // Taken before the row is read, so a status change and any change to the session's inputs happen one after the other.
    private void lockRow(Long id) {
        jdbc.sql("SELECT id FROM exam_session WHERE id = :id FOR UPDATE").param("id", id).query(Long.class).optional();
    }

    private void apply(ExamSession session, ExamSessionRequest request) {
        LocalDate start = request.startDate();
        LocalDate end = request.endDate();
        if (end.isBefore(start)) throw ApiException.unprocessable("INVALID_DATES", "A session cannot end before it starts.");
        if (ChronoUnit.DAYS.between(start, end) > MAX_SESSION_DAYS) {
            throw ApiException.unprocessable("INVALID_DATES", "A session can run for at most " + MAX_SESSION_DAYS + " days.");
        }
        // Dates already saved may have passed by now. Only newly chosen ones must be in the future.
        LocalDate today = today();
        if ((!start.equals(session.getStartDate()) && start.isBefore(today)) || (!end.equals(session.getEndDate()) && end.isBefore(today))) {
            throw ApiException.unprocessable("PAST_DATE", "A session cannot be set in the past.");
        }
        if ((!start.equals(session.getStartDate()) && isWeekend(start)) || (!end.equals(session.getEndDate()) && isWeekend(end))) {
            throw ApiException.unprocessable("WEEKEND_DATE", "A session must start and end on a weekday, because the college is closed at weekends.");
        }
        if (start.isAfter(today.plusYears(2))) {
            throw ApiException.unprocessable("INVALID_DATES", "A session cannot start more than two years ahead.");
        }
        String name = request.name().trim();
        long others = session.getId() == null ? 0L : session.getId();
        if (sessions.existsByNameIgnoreCaseAndIdNot(name, others)) {
            throw ApiException.conflict("DUPLICATE", "Another session is already called " + name + ".");
        }
        session.setName(name);
        session.setStartDate(start);
        session.setEndDate(end);
        session.setMaxExamsPerStudentPerDay(request.maxExamsPerStudentPerDay());
        session.setHeavyGapDays(request.heavyGapDays());
        if (request.studentsPerInvigilator() != null) session.setStudentsPerInvigilator(request.studentsPerInvigilator());
        if (request.maxDutiesPerInvigilator() != null) session.setMaxDutiesPerInvigilator(request.maxDutiesPerInvigilator());
    }

    private ExamSessionResponse withCounts(ExamSession session) {
        List<Long> id = List.of(session.getId());
        return ExamSessionResponse.from(session,
                QuerySupport.counts(id, sessions::slotCounts).getOrDefault(session.getId(), 0L),
                QuerySupport.counts(id, sessions::examinationCounts).getOrDefault(session.getId(), 0L));
    }

    /** The session itself, for services that read it without changing it. */
    public ExamSession find(Long id) {
        return sessions.findById(id).orElseThrow(() -> ApiException.notFound("Exam session", id));
    }
}
