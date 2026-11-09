package com.slotforge.academic.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.slotforge.academic.dto.DepartmentRequest;
import com.slotforge.academic.dto.DepartmentResponse;
import com.slotforge.academic.entity.Department;
import com.slotforge.academic.repository.CourseRepository;
import com.slotforge.academic.repository.DepartmentRepository;
import com.slotforge.academic.repository.SubjectRepository;
import com.slotforge.audit.service.AuditService;
import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.common.exception.ApiException;
import com.slotforge.common.persistence.QuerySupport;
import com.slotforge.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private static final Set<String> SORTABLE = Set.of("code", "name");

    private final DepartmentRepository departments;
    private final CourseRepository courses;
    private final SubjectRepository subjects;
    private final AuditService audit;
    private final AppUserRepository users;

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> search(String q, Pageable pageable) {
        QuerySupport.requireSortable(pageable, SORTABLE);
        Specification<Department> spec = (root, query, cb) -> {
            if (!StringUtils.hasText(q)) return cb.conjunction();
            String pattern = QuerySupport.containsPattern(q);
            return cb.or(cb.like(cb.lower(root.get("code")), pattern, '\\'),
                    cb.like(cb.lower(root.get("name")), pattern, '\\'));
        };
        Page<Department> page = departments.findAll(spec, pageable);
        List<Long> ids = page.getContent().stream().map(Department::getId).toList();
        Map<Long, Long> courseCounts = QuerySupport.counts(ids, courses::courseCounts);
        Map<Long, Long> subjectCounts = QuerySupport.counts(ids, subjects::subjectCounts);
        return PageResponse.from(page.map(d -> DepartmentResponse.from(d,
                courseCounts.getOrDefault(d.getId(), 0L), subjectCounts.getOrDefault(d.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public DepartmentResponse get(Long id) {
        return withCounts(find(id));
    }

    @Transactional
    public DepartmentResponse create(Long actorId, DepartmentRequest request) {
        Department department = new Department();
        apply(department, request);
        departments.save(department);
        audit.record(actorId, "DEPARTMENT_CREATED", "department", department.getId(), codeJson(department));
        return DepartmentResponse.from(department, 0, 0);
    }

    @Transactional
    public DepartmentResponse update(Long actorId, Long id, DepartmentRequest request) {
        Department department = find(id);
        apply(department, request);
        audit.record(actorId, "DEPARTMENT_UPDATED", "department", id, codeJson(department));
        return withCounts(department);
    }

    @Transactional
    public DepartmentResponse appointHead(Long actorId, Long id, Long userId) {
        Department department = find(id);
        AppUser head = userId == null ? null : users.findById(userId)
                .filter(candidate -> candidate.isActive() && candidate.getRoles().contains(Role.HOD))
                .orElseThrow(() -> ApiException.unprocessable("NOT_A_HEAD", "Only an active account with the Head of department role can be appointed."));
        department.setHead(head);
        audit.record(actorId, "DEPARTMENT_HEAD_CHANGED", "department", id, "{\"head\":" + userId + "}");
        return withCounts(department);
    }

    /** Someone who stops being a head stops approving at once. The page then says the post is vacant. */
    @Transactional
    public void vacateHead(Long actorId, Long userId) {
        if (departments.vacateHead(userId) > 0) audit.record(actorId, "DEPARTMENT_HEAD_VACATED", "app_user", userId, null);
    }

    @Transactional
    public void delete(Long actorId, Long id) {
        Department department = find(id);
        try {
            departments.delete(department);
            departments.flush();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("IN_USE",
                    department.getName() + " still has programmes or subjects. Move or delete them first.");
        }
        audit.record(actorId, "DEPARTMENT_DELETED", "department", id, codeJson(department));
    }

    private void apply(Department department, DepartmentRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        String name = request.name().trim();
        // Identity ids start at 1, so 0 means "compare with every row" when creating.
        long others = department.getId() == null ? 0L : department.getId();
        if (departments.existsByCodeIgnoreCaseAndIdNot(code, others)) {
            throw ApiException.conflict("DUPLICATE", "Another department already uses the code " + code + ".");
        }
        if (departments.existsByNameIgnoreCaseAndIdNot(name, others)) {
            throw ApiException.conflict("DUPLICATE", "Another department is already called " + name + ".");
        }
        department.setCode(code);
        department.setName(name);
    }

    private Department find(Long id) {
        return departments.findById(id).orElseThrow(() -> ApiException.notFound("Department", id));
    }

    private DepartmentResponse withCounts(Department department) {
        List<Long> id = List.of(department.getId());
        return DepartmentResponse.from(department, QuerySupport.counts(id, courses::courseCounts).getOrDefault(department.getId(), 0L),
                QuerySupport.counts(id, subjects::subjectCounts).getOrDefault(department.getId(), 0L));
    }

    // Codes are validated to letters, digits and hyphens, so this is always valid JSON.
    private static String codeJson(Department department) {
        return "{\"code\":\"" + department.getCode() + "\"}";
    }
}
