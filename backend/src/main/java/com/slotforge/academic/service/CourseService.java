package com.slotforge.academic.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.slotforge.academic.dto.CourseRequest;
import com.slotforge.academic.dto.CourseResponse;
import com.slotforge.academic.entity.Course;
import com.slotforge.academic.repository.CourseRepository;
import com.slotforge.academic.repository.CurriculumRepository;
import com.slotforge.academic.repository.DepartmentRepository;
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
public class CourseService {

    private static final Set<String> SORTABLE = Set.of("code", "name", "totalSemesters");

    private final CourseRepository courses;
    private final DepartmentRepository departments;
    private final CurriculumRepository curricula;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> search(String q, Long departmentId, Pageable pageable) {
        QuerySupport.requireSortable(pageable, SORTABLE);
        Specification<Course> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (StringUtils.hasText(q)) {
                String pattern = QuerySupport.containsPattern(q);
                where.add(cb.or(cb.like(cb.lower(root.get("code")), pattern, '\\'),
                        cb.like(cb.lower(root.get("name")), pattern, '\\')));
            }
            if (departmentId != null) where.add(cb.equal(root.get("department").get("id"), departmentId));
            return cb.and(where.toArray(Predicate[]::new));
        };
        var page = courses.findAll(spec, pageable);
        Map<Long, Long> counts = QuerySupport.counts(page.getContent().stream().map(Course::getId).toList(), curricula::curriculumCounts);
        return PageResponse.from(page.map(c -> CourseResponse.from(c, counts.getOrDefault(c.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public CourseResponse get(Long id) {
        return withCount(find(id));
    }

    @Transactional
    public CourseResponse create(Long actorId, CourseRequest request) {
        Course course = new Course();
        apply(course, request);
        courses.save(course);
        audit.record(actorId, "COURSE_CREATED", "course", course.getId(), codeJson(course));
        return CourseResponse.from(course, 0);
    }

    @Transactional
    public CourseResponse update(Long actorId, Long id, CourseRequest request) {
        Course course = find(id);
        // Shortening a programme must not strand subjects or batches in semesters it no longer has.
        int inUse = courses.highestSemesterInUse(id);
        if (request.totalSemesters() < inUse) {
            throw ApiException.unprocessable("SEMESTERS_IN_USE",
                    "Semester " + inUse + " is still used by a curriculum or batch of this programme.");
        }
        apply(course, request);
        audit.record(actorId, "COURSE_UPDATED", "course", id, codeJson(course));
        return withCount(course);
    }

    @Transactional
    public void delete(Long actorId, Long id) {
        Course course = find(id);
        try {
            courses.delete(course);
            courses.flush();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("IN_USE", course.getCode() + " still has curricula. Delete them first.");
        }
        audit.record(actorId, "COURSE_DELETED", "course", id, codeJson(course));
    }

    private void apply(Course course, CourseRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        long others = course.getId() == null ? 0L : course.getId();
        if (courses.existsByCodeIgnoreCaseAndIdNot(code, others)) {
            throw ApiException.conflict("DUPLICATE", "Another programme already uses the code " + code + ".");
        }
        course.setDepartment(departments.findById(request.departmentId())
                .orElseThrow(() -> ApiException.unprocessable("UNKNOWN_DEPARTMENT", "Choose a department from the list.")));
        course.setCode(code);
        course.setName(request.name().trim());
        course.setTotalSemesters(request.totalSemesters());
    }

    private CourseResponse withCount(Course course) {
        return CourseResponse.from(course,
                QuerySupport.counts(List.of(course.getId()), curricula::curriculumCounts).getOrDefault(course.getId(), 0L));
    }

    private Course find(Long id) {
        return courses.findById(id).orElseThrow(() -> ApiException.notFound("Programme", id));
    }

    private static String codeJson(Course course) {
        return "{\"code\":\"" + course.getCode() + "\"}";
    }
}
