package com.slotforge.academic.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.slotforge.academic.dto.SubjectRequest;
import com.slotforge.academic.dto.SubjectResponse;
import com.slotforge.academic.entity.Subject;
import com.slotforge.academic.repository.DepartmentRepository;
import com.slotforge.academic.repository.SubjectRepository;
import com.slotforge.audit.service.AuditService;
import com.slotforge.common.exception.ApiException;
import com.slotforge.common.persistence.QuerySupport;
import com.slotforge.common.response.PageResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private static final Set<String> SORTABLE = Set.of("code", "name");

    private final SubjectRepository subjects;
    private final DepartmentRepository departments;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<SubjectResponse> search(String q, Long departmentId, Pageable pageable) {
        QuerySupport.requireSortable(pageable, SORTABLE);
        Specification<Subject> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (StringUtils.hasText(q)) {
                String pattern = QuerySupport.containsPattern(q);
                where.add(cb.or(cb.like(cb.lower(root.get("code")), pattern, '\\'),
                        cb.like(cb.lower(root.get("name")), pattern, '\\')));
            }
            if (departmentId != null) where.add(cb.equal(root.get("department").get("id"), departmentId));
            return cb.and(where.toArray(Predicate[]::new));
        };
        var page = subjects.findAll(spec, pageable);
        Map<Long, Long> counts = QuerySupport.counts(page.getContent().stream().map(Subject::getId).toList(),
                subjects::curriculumCounts);
        return PageResponse.from(page.map(s -> SubjectResponse.from(s, counts.getOrDefault(s.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public SubjectResponse get(Long id) {
        return withCount(find(id));
    }

    @Transactional
    public SubjectResponse create(Long actorId, SubjectRequest request) {
        Subject subject = new Subject();
        apply(subject, request);
        subjects.save(subject);
        audit.record(actorId, "SUBJECT_CREATED", "subject", subject.getId(), codeJson(subject));
        return SubjectResponse.from(subject, 0);
    }

    @Transactional
    public SubjectResponse update(Long actorId, Long id, SubjectRequest request) {
        Subject subject = find(id);
        apply(subject, request);
        audit.record(actorId, "SUBJECT_UPDATED", "subject", id, codeJson(subject));
        return withCount(subject);
    }

    @Transactional
    public void delete(Long actorId, Long id) {
        Subject subject = find(id);
        try {
            subjects.delete(subject);
            subjects.flush();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("IN_USE",
                    subject.getCode() + " is part of a curriculum or an exam. Remove it from those first.");
        }
        audit.record(actorId, "SUBJECT_DELETED", "subject", id, codeJson(subject));
    }

    private void apply(Subject subject, SubjectRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        long others = subject.getId() == null ? 0L : subject.getId();
        if (subjects.existsByCodeIgnoreCaseAndIdNot(code, others)) {
            throw ApiException.conflict("DUPLICATE", "Another subject already uses the code " + code + ".");
        }
        subject.setDepartment(departments.findById(request.departmentId())
                .orElseThrow(() -> ApiException.unprocessable("UNKNOWN_DEPARTMENT", "Choose a department from the list.")));
        subject.setCode(code);
        subject.setName(request.name().trim());
        subject.setHeavy(request.heavy());
    }

    private SubjectResponse withCount(Subject subject) {
        return SubjectResponse.from(subject,
                QuerySupport.counts(List.of(subject.getId()), subjects::curriculumCounts).getOrDefault(subject.getId(), 0L));
    }

    private Subject find(Long id) {
        return subjects.findById(id).orElseThrow(() -> ApiException.notFound("Subject", id));
    }

    private static String codeJson(Subject subject) {
        return "{\"code\":\"" + subject.getCode() + "\"}";
    }
}
