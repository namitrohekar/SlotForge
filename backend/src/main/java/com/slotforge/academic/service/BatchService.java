package com.slotforge.academic.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slotforge.academic.dto.BatchRequest;
import com.slotforge.academic.dto.BatchResponse;
import com.slotforge.academic.entity.Batch;
import com.slotforge.academic.repository.BatchRepository;
import com.slotforge.academic.repository.CurriculumRepository;
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
public class BatchService {

    private static final Set<String> SORTABLE = Set.of("name", "admissionYear", "currentSemester");

    private final BatchRepository batches;
    private final CurriculumRepository curricula;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<BatchResponse> search(String q, Long courseId, Pageable pageable) {
        QuerySupport.requireSortable(pageable, SORTABLE);
        Specification<Batch> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (StringUtils.hasText(q)) where.add(cb.like(cb.lower(root.get("name")), QuerySupport.containsPattern(q), '\\'));
            if (courseId != null) where.add(cb.equal(root.get("curriculum").get("course").get("id"), courseId));
            return cb.and(where.toArray(Predicate[]::new));
        };
        var page = batches.findAll(spec, pageable);
        Map<Long, Long> counts = QuerySupport.counts(page.getContent().stream().map(Batch::getId).toList(), batches::studentCounts);
        return PageResponse.from(page.map(b -> BatchResponse.from(b, counts.getOrDefault(b.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public BatchResponse get(Long id) {
        return withCount(find(id));
    }

    @Transactional
    public BatchResponse create(Long actorId, BatchRequest request) {
        Batch batch = new Batch();
        batch.setCurriculum(curricula.findById(request.curriculumId())
                .orElseThrow(() -> ApiException.unprocessable("UNKNOWN_CURRICULUM", "Choose a curriculum from the list.")));
        apply(batch, request);
        batches.save(batch);
        audit.record(actorId, "BATCH_CREATED", "batch", batch.getId(), null);
        return BatchResponse.from(batch, 0);
    }

    @Transactional
    public BatchResponse update(Long actorId, Long id, BatchRequest request) {
        Batch batch = find(id);
        // A batch keeps the syllabus it was admitted under. A wrong choice is fixed by recreating it.
        if (!batch.getCurriculum().getId().equals(request.curriculumId())) {
            throw ApiException.unprocessable("CURRICULUM_FIXED", "A batch stays with its curriculum.");
        }
        apply(batch, request);
        audit.record(actorId, "BATCH_UPDATED", "batch", id, null);
        return withCount(batch);
    }

    @Transactional
    public void delete(Long actorId, Long id) {
        Batch batch = find(id);
        try {
            batches.delete(batch);
            batches.flush();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("IN_USE", batch.getName() + " still has students. Move or delete them first.");
        }
        audit.record(actorId, "BATCH_DELETED", "batch", id, null);
    }

    private void apply(Batch batch, BatchRequest request) {
        String name = request.name().trim();
        long others = batch.getId() == null ? 0L : batch.getId();
        var curriculum = batch.getCurriculum();
        if (batches.existsByCurriculumIdAndNameIgnoreCaseAndIdNot(curriculum.getId(), name, others)) {
            throw ApiException.conflict("DUPLICATE", curriculum.getName() + " already has a batch called " + name + ".");
        }
        int semesters = curriculum.getCourse().getTotalSemesters();
        if (request.currentSemester() > semesters) {
            throw ApiException.unprocessable("SEMESTER_OUT_OF_RANGE",
                    curriculum.getCourse().getCode() + " has " + semesters + " semesters, not " + request.currentSemester() + ".");
        }
        // A batch can be set up ahead of its intake, but not further ahead than next year's.
        int latestIntake = LocalDate.now(ZoneId.of("Asia/Kolkata")).getYear() + 1;
        if (request.admissionYear() > latestIntake) {
            throw ApiException.unprocessable("INVALID_ADMISSION_YEAR", "The admission year cannot be later than " + latestIntake + ".");
        }
        batch.setName(name);
        batch.setAdmissionYear(request.admissionYear());
        batch.setCurrentSemester(request.currentSemester());
    }

    private BatchResponse withCount(Batch batch) {
        return BatchResponse.from(batch,
                QuerySupport.counts(List.of(batch.getId()), batches::studentCounts).getOrDefault(batch.getId(), 0L));
    }

    private Batch find(Long id) {
        return batches.findById(id).orElseThrow(() -> ApiException.notFound("Batch", id));
    }
}
