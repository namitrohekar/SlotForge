package com.slotforge.academic.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.slotforge.academic.dto.CurriculumRequest;
import com.slotforge.academic.dto.CurriculumResponse;
import com.slotforge.academic.dto.CurriculumSubjectRequest;
import com.slotforge.academic.dto.CurriculumSubjectResponse;
import com.slotforge.academic.entity.Curriculum;
import com.slotforge.academic.entity.CurriculumSubject;
import com.slotforge.academic.entity.Subject;
import com.slotforge.academic.repository.CourseRepository;
import com.slotforge.academic.repository.CurriculumRepository;
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
public class CurriculumService {

    private static final Set<String> SORTABLE = Set.of("name", "effectiveFromYear");

    private final CurriculumRepository curricula;
    private final CourseRepository courses;
    private final SubjectRepository subjects;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<CurriculumResponse> search(String q, Long courseId, Pageable pageable) {
        QuerySupport.requireSortable(pageable, SORTABLE);
        Specification<Curriculum> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (StringUtils.hasText(q)) where.add(cb.like(cb.lower(root.get("name")), QuerySupport.containsPattern(q), '\\'));
            if (courseId != null) where.add(cb.equal(root.get("course").get("id"), courseId));
            return cb.and(where.toArray(Predicate[]::new));
        };
        var page = curricula.findAll(spec, pageable);
        Map<Long, long[]> usage = usage(page.getContent().stream().map(Curriculum::getId).toList());
        return PageResponse.from(page.map(c -> respond(c, usage)));
    }

    @Transactional(readOnly = true)
    public CurriculumResponse get(Long id) {
        Curriculum curriculum = find(id);
        return respond(curriculum, usage(List.of(id)));
    }

    @Transactional
    public CurriculumResponse create(Long actorId, CurriculumRequest request) {
        Curriculum curriculum = new Curriculum();
        curriculum.setCourse(courses.findById(request.courseId())
                .orElseThrow(() -> ApiException.unprocessable("UNKNOWN_COURSE", "Choose a programme from the list.")));
        apply(curriculum, request);
        curricula.save(curriculum);
        audit.record(actorId, "CURRICULUM_CREATED", "curriculum", curriculum.getId(), null);
        return CurriculumResponse.from(curriculum, 0, 0);
    }

    @Transactional
    public CurriculumResponse update(Long actorId, Long id, CurriculumRequest request) {
        Curriculum curriculum = find(id);
        // Its subjects are placed by that programme's semesters, so a curriculum never changes programme.
        if (!curriculum.getCourse().getId().equals(request.courseId())) {
            throw ApiException.unprocessable("COURSE_FIXED",
                    "A curriculum stays with its programme. Create a new curriculum for the other programme.");
        }
        apply(curriculum, request);
        audit.record(actorId, "CURRICULUM_UPDATED", "curriculum", id, null);
        return respond(curriculum, usage(List.of(id)));
    }

    @Transactional
    public void delete(Long actorId, Long id) {
        Curriculum curriculum = find(id);
        try {
            curricula.delete(curriculum);
            curricula.flush();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("IN_USE", curriculum.getName() + " is followed by a batch. It cannot be deleted.");
        }
        audit.record(actorId, "CURRICULUM_DELETED", "curriculum", id, null);
    }

    @Transactional(readOnly = true)
    public List<CurriculumSubjectResponse> subjects(Long id) {
        return describe(find(id).getSubjects());
    }

    /** Replaces the whole placement, so the list sent is exactly what the curriculum holds afterwards. */
    @Transactional
    public List<CurriculumSubjectResponse> replaceSubjects(Long actorId, Long id, List<CurriculumSubjectRequest> placements) {
        Curriculum curriculum = find(id);
        int semesters = curriculum.getCourse().getTotalSemesters();
        Set<Long> seen = new HashSet<>();
        for (CurriculumSubjectRequest placement : placements) {
            if (!seen.add(placement.subjectId())) {
                throw ApiException.unprocessable("DUPLICATE_SUBJECT", "A subject can appear only once in a curriculum.");
            }
            if (placement.semesterNo() > semesters) {
                throw ApiException.unprocessable("SEMESTER_OUT_OF_RANGE",
                        curriculum.getCourse().getCode() + " has " + semesters + " semesters, not " + placement.semesterNo() + ".");
            }
        }
        if (subjects.findAllById(seen).size() != seen.size()) {
            throw ApiException.unprocessable("UNKNOWN_SUBJECT", "One of the subjects no longer exists. Reload and try again.");
        }

        curriculum.getSubjects().clear();
        placements.forEach(p -> curriculum.getSubjects().add(new CurriculumSubject(p.subjectId(), p.semesterNo(), p.elective())));
        audit.record(actorId, "CURRICULUM_SUBJECTS_CHANGED", "curriculum", id, "{\"subjects\":" + placements.size() + "}");
        return describe(curriculum.getSubjects());
    }

    private void apply(Curriculum curriculum, CurriculumRequest request) {
        String name = request.name().trim();
        long others = curriculum.getId() == null ? 0L : curriculum.getId();
        if (curricula.existsByCourseIdAndNameIgnoreCaseAndIdNot(curriculum.getCourse().getId(), name, others)) {
            throw ApiException.conflict("DUPLICATE", curriculum.getCourse().getCode() + " already has a curriculum called " + name + ".");
        }
        curriculum.setName(name);
        curriculum.setEffectiveFromYear(request.effectiveFromYear());
    }

    private List<CurriculumSubjectResponse> describe(Set<CurriculumSubject> placements) {
        Map<Long, Subject> byId = subjects.findByIdIn(placements.stream().map(CurriculumSubject::getSubjectId).toList())
                .stream().collect(Collectors.toMap(Subject::getId, Function.identity()));
        return placements.stream()
                .filter(p -> byId.containsKey(p.getSubjectId()))
                .map(p -> {
                    Subject s = byId.get(p.getSubjectId());
                    return new CurriculumSubjectResponse(s.getId(), s.getCode(), s.getName(), s.isHeavy(),
                            s.getDepartment().getCode(), p.getSemesterNo(), p.isElective());
                })
                .sorted(Comparator.comparingInt(CurriculumSubjectResponse::semesterNo)
                        .thenComparing(CurriculumSubjectResponse::elective)
                        .thenComparing(CurriculumSubjectResponse::code))
                .toList();
    }

    private Map<Long, long[]> usage(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return curricula.usage(ids).stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> new long[] {((Number) row[1]).longValue(), ((Number) row[2]).longValue()}));
    }

    private static CurriculumResponse respond(Curriculum curriculum, Map<Long, long[]> usage) {
        long[] counts = Objects.requireNonNullElse(usage.get(curriculum.getId()), new long[] {0, 0});
        return CurriculumResponse.from(curriculum, counts[0], counts[1]);
    }

    private Curriculum find(Long id) {
        return curricula.findById(id).orElseThrow(() -> ApiException.notFound("Curriculum", id));
    }
}
