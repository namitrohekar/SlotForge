package com.slotforge.student.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.slotforge.academic.repository.BatchRepository;
import com.slotforge.audit.service.AuditService;
import com.slotforge.common.exception.ApiException;
import com.slotforge.common.persistence.QuerySupport;
import com.slotforge.common.response.PageResponse;
import com.slotforge.student.dto.StudentRequest;
import com.slotforge.student.dto.StudentResponse;
import com.slotforge.student.entity.Student;
import com.slotforge.student.repository.StudentRepository;
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
public class StudentService {

    private static final Set<String> SORTABLE = Set.of("rollNo", "fullName");

    private final StudentRepository students;
    private final BatchRepository batches;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> search(String q, Long batchId, Pageable pageable) {
        QuerySupport.requireSortable(pageable, SORTABLE);
        Specification<Student> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (StringUtils.hasText(q)) {
                String pattern = QuerySupport.containsPattern(q);
                where.add(cb.or(cb.like(cb.lower(root.get("rollNo")), pattern, '\\'),
                        cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("email")), pattern, '\\')));
            }
            if (batchId != null) where.add(cb.equal(root.get("batch").get("id"), batchId));
            return cb.and(where.toArray(Predicate[]::new));
        };
        var page = students.findAll(spec, pageable);
        Map<Long, Long> counts = QuerySupport.counts(page.getContent().stream().map(Student::getId).toList(), students::enrolmentCounts);
        return PageResponse.from(page.map(s -> StudentResponse.from(s, counts.getOrDefault(s.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public StudentResponse get(Long id) {
        return withCount(find(id));
    }

    @Transactional
    public StudentResponse create(Long actorId, StudentRequest request) {
        Student student = new Student();
        apply(student, request);
        students.save(student);
        audit.record(actorId, "STUDENT_CREATED", "student", student.getId(), null);
        return StudentResponse.from(student, 0);
    }

    @Transactional
    public StudentResponse update(Long actorId, Long id, StudentRequest request) {
        Student student = find(id);
        apply(student, request);
        audit.record(actorId, "STUDENT_UPDATED", "student", id, null);
        return withCount(student);
    }

    @Transactional
    public void delete(Long actorId, Long id) {
        Student student = find(id);
        try {
            students.delete(student);
            students.flush();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("IN_USE", student.getRollNo() + " is enrolled in an exam. Remove the enrolment first.");
        }
        audit.record(actorId, "STUDENT_DELETED", "student", id, null);
    }

    private void apply(Student student, StudentRequest request) {
        String rollNo = request.rollNo().trim().toUpperCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        long others = student.getId() == null ? 0L : student.getId();
        if (students.existsByRollNoIgnoreCaseAndIdNot(rollNo, others)) {
            throw ApiException.conflict("DUPLICATE", "Another student already has roll number " + rollNo + ".");
        }
        if (students.existsByEmailAndIdNot(email, others)) {
            throw ApiException.conflict("DUPLICATE", "Another student already uses " + email + ".");
        }
        student.setBatch(batches.findById(request.batchId())
                .orElseThrow(() -> ApiException.unprocessable("UNKNOWN_BATCH", "Choose a batch from the list.")));
        student.setRollNo(rollNo);
        student.setFullName(request.fullName().trim());
        student.setEmail(email);
    }

    private StudentResponse withCount(Student student) {
        return StudentResponse.from(student,
                QuerySupport.counts(List.of(student.getId()), students::enrolmentCounts).getOrDefault(student.getId(), 0L));
    }

    private Student find(Long id) {
        return students.findById(id).orElseThrow(() -> ApiException.notFound("Student", id));
    }
}
