package com.slotforge.exam.service;

import java.util.List;
import java.util.Set;

import com.slotforge.audit.service.AuditService;
import com.slotforge.common.exception.ApiException;
import com.slotforge.common.persistence.QuerySupport;
import com.slotforge.common.response.PageResponse;
import com.slotforge.exam.dto.EnrolledStudentResponse;
import com.slotforge.exam.dto.EnrolmentResultResponse;
import com.slotforge.exam.entity.Examination;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Enrolment is the highest-volume input and only ever read as rows, so it uses SQL directly. */
@Service
@RequiredArgsConstructor
public class EnrolmentService {

    private static final String MATCHES = """
            FROM examination_enrolment e
            JOIN student s ON s.id = e.student_id
            JOIN batch b ON b.id = s.batch_id
            JOIN curriculum cu ON cu.id = b.curriculum_id
            JOIN course c ON c.id = cu.course_id
            WHERE e.examination_id = :exam
              AND (lower(s.roll_no) LIKE :pattern ESCAPE '\\' OR lower(s.full_name) LIKE :pattern ESCAPE '\\')
            """;

    private final JdbcClient jdbc;
    private final ExaminationService examinations;
    private final ExamSessionService sessions;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<EnrolledStudentResponse> list(Long examinationId, String q, Pageable pageable) {
        examinations.find(examinationId);
        String pattern = QuerySupport.containsPattern(q == null ? "" : q);
        long total = jdbc.sql("SELECT count(*) " + MATCHES)
                .param("exam", examinationId).param("pattern", pattern).query(Long.class).single();
        List<EnrolledStudentResponse> items = jdbc.sql("""
                SELECT s.id, s.roll_no, s.full_name, c.code, b.name
                """ + MATCHES + "ORDER BY s.roll_no LIMIT :size OFFSET :offset")
                .param("exam", examinationId).param("pattern", pattern)
                .param("size", pageable.getPageSize()).param("offset", pageable.getOffset())
                .query((row, index) -> new EnrolledStudentResponse(
                        row.getLong(1), row.getString(2), row.getString(3), row.getString(4), row.getString(5)))
                .list();
        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());
        return new PageResponse<>(items, pageable.getPageNumber(), pageable.getPageSize(), total, totalPages);
    }

    /** Students already enrolled, or no longer active, are left out. The result says how many joined. */
    @Transactional
    public EnrolmentResultResponse add(Long actorId, Long examinationId, Set<Long> studentIds) {
        Examination exam = examinations.find(examinationId);
        sessions.requirePlanning(exam.getSession().getId());
        int added = jdbc.sql("""
                INSERT INTO examination_enrolment (examination_id, student_id)
                SELECT CAST(:exam AS BIGINT), id FROM student WHERE id IN (:students) AND active
                ON CONFLICT DO NOTHING""")
                .param("exam", examinationId).param("students", studentIds).update();
        examinations.refreshStatus(exam);
        audit.record(actorId, "ENROLMENT_ADDED", "examination", examinationId, "{\"added\":" + added + "}");
        return new EnrolmentResultResponse(added);
    }

    @Transactional
    public void remove(Long actorId, Long examinationId, Long studentId) {
        Examination exam = examinations.find(examinationId);
        sessions.requirePlanning(exam.getSession().getId());
        int removed;
        try {
            removed = jdbc.sql("DELETE FROM examination_enrolment WHERE examination_id = :exam AND student_id = :student")
                    .param("exam", examinationId).param("student", studentId).update();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("IN_USE", "This student is placed in a hall in a timetable. Delete that timetable first.");
        }
        if (removed == 0) throw ApiException.notFound("Enrolment for student", studentId);
        examinations.refreshStatus(exam);
        audit.record(actorId, "ENROLMENT_REMOVED", "examination", examinationId, "{\"student\":" + studentId + "}");
    }
}
