package com.slotforge.exam.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.slotforge.academic.entity.Batch;
import com.slotforge.academic.entity.CurriculumSubject;
import com.slotforge.academic.repository.BatchRepository;
import com.slotforge.academic.repository.SubjectRepository;
import com.slotforge.audit.service.AuditService;
import com.slotforge.common.exception.ApiException;
import com.slotforge.common.persistence.QuerySupport;
import com.slotforge.common.response.PageResponse;
import com.slotforge.exam.dto.ExaminationResponse;
import com.slotforge.exam.dto.ExaminationUpdateRequest;
import com.slotforge.exam.dto.FromCurriculumRequest;
import com.slotforge.exam.dto.FromCurriculumResponse;
import com.slotforge.exam.entity.ExamSession;
import com.slotforge.exam.entity.Examination;
import com.slotforge.exam.entity.ExaminationStatus;
import com.slotforge.exam.repository.ExaminationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ExaminationService {

    private static final Set<String> SORTABLE = Set.of("subject.code", "subject.name", "durationMinutes");

    private final ExaminationRepository examinations;
    private final ExamSessionService sessions;
    private final BatchRepository batches;
    private final SubjectRepository subjects;
    private final AuditService audit;
    private final JdbcClient jdbc;

    @Transactional(readOnly = true)
    public PageResponse<ExaminationResponse> search(Long sessionId, String q, Pageable pageable) {
        QuerySupport.requireSortable(pageable, SORTABLE);
        sessions.get(sessionId);
        Specification<Examination> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            where.add(cb.equal(root.get("session").get("id"), sessionId));
            if (StringUtils.hasText(q)) {
                String pattern = QuerySupport.containsPattern(q);
                where.add(cb.or(cb.like(cb.lower(root.get("subject").get("code")), pattern, '\\'),
                        cb.like(cb.lower(root.get("subject").get("name")), pattern, '\\')));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
        var page = examinations.findAll(spec, pageable);
        Map<Long, Long> counts = QuerySupport.counts(page.getContent().stream().map(Examination::getId).toList(), examinations::enrolmentCounts);
        return PageResponse.from(page.map(e -> ExaminationResponse.from(e, counts.getOrDefault(e.getId(), 0L))));
    }

    /**
     * Creates each batch's papers for its current semester. Core papers enrol the whole batch, electives wait
     * for individual students, and a paper shared by several batches stays one exam. Safe to repeat.
     */
    @Transactional
    public FromCurriculumResponse createFromCurriculum(Long actorId, Long sessionId, FromCurriculumRequest request) {
        ExamSession session = sessions.requirePlanning(sessionId);
        Map<Long, Examination> bySubject = new HashMap<>();
        examinations.findBySessionId(sessionId).forEach(exam -> bySubject.put(exam.getSubject().getId(), exam));

        int created = 0;
        int enrolled = 0;
        int electives = 0;
        List<Examination> touched = new ArrayList<>();
        for (Long batchId : new TreeSet<>(request.batchIds())) {
            Batch batch = batches.findById(batchId)
                    .orElseThrow(() -> ApiException.unprocessable("UNKNOWN_BATCH", "One of the batches no longer exists. Reload and try again."));
            List<CurriculumSubject> papers = batch.getCurriculum().getSubjects().stream()
                    .filter(paper -> paper.getSemesterNo() == batch.getCurrentSemester())
                    .sorted(Comparator.comparing(CurriculumSubject::getSubjectId))
                    .toList();
            for (CurriculumSubject paper : papers) {
                Examination exam = bySubject.get(paper.getSubjectId());
                if (exam == null) {
                    exam = new Examination();
                    exam.setSession(session);
                    exam.setSubject(subjects.getReferenceById(paper.getSubjectId()));
                    exam.setDurationMinutes(request.durationMinutes());
                    examinations.save(exam);
                    bySubject.put(paper.getSubjectId(), exam);
                    created++;
                    if (paper.isElective()) electives++;
                }
                if (!paper.isElective()) enrolled += enrolBatch(exam.getId(), batchId);
                touched.add(exam);
            }
        }
        touched.forEach(this::refreshStatus);
        audit.record(actorId, "EXAMS_FROM_CURRICULUM", "exam_session", sessionId, "{\"created\":" + created + ",\"enrolled\":" + enrolled + "}");
        return new FromCurriculumResponse(created, enrolled, electives);
    }

    private int enrolBatch(Long examinationId, Long batchId) {
        return jdbc.sql("""
                INSERT INTO examination_enrolment (examination_id, student_id)
                SELECT CAST(:exam AS BIGINT), id FROM student WHERE batch_id = :batch AND active
                ON CONFLICT DO NOTHING""")
                .param("exam", examinationId).param("batch", batchId).update();
    }

    @Transactional
    public ExaminationResponse update(Long actorId, Long id, ExaminationUpdateRequest request) {
        Examination exam = find(id);
        ExamSession session = sessions.requirePlanning(exam.getSession().getId());
        var earliest = request.earliestDate();
        var latest = request.latestDate();
        if ((earliest != null && latest != null && earliest.isAfter(latest))
                || (earliest != null && (earliest.isBefore(session.getStartDate()) || earliest.isAfter(session.getEndDate())))
                || (latest != null && (latest.isBefore(session.getStartDate()) || latest.isAfter(session.getEndDate())))) {
            throw ApiException.unprocessable("INVALID_WINDOW", "The date window must be in order and inside the session.");
        }
        if ((earliest != null && ExamSessionService.isWeekend(earliest)) || (latest != null && ExamSessionService.isWeekend(latest))) {
            throw ApiException.unprocessable("WEEKEND_DATE", "The date window must start and end on a weekday.");
        }
        exam.setDurationMinutes(request.durationMinutes());
        exam.setEarliestDate(earliest);
        exam.setLatestDate(latest);
        audit.record(actorId, "EXAM_UPDATED", "examination", id, null);
        return ExaminationResponse.from(exam, examinations.enrolled(id));
    }

    @Transactional
    public void delete(Long actorId, Long id) {
        Examination exam = find(id);
        sessions.requirePlanning(exam.getSession().getId());
        try {
            examinations.delete(exam);
            examinations.flush();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("IN_USE", "This paper is in a timetable. Delete that timetable first.");
        }
        audit.record(actorId, "EXAM_DELETED", "examination", id, null);
    }

    /** A paper is ready for the scheduler exactly when it has students. */
    public void refreshStatus(Examination exam) {
        if (exam.getStatus() == ExaminationStatus.DRAFT || exam.getStatus() == ExaminationStatus.READY) {
            exam.setStatus(examinations.enrolled(exam.getId()) > 0 ? ExaminationStatus.READY : ExaminationStatus.DRAFT);
        }
    }

    public Examination find(Long id) {
        return examinations.findById(id).orElseThrow(() -> ApiException.notFound("Examination", id));
    }
}
