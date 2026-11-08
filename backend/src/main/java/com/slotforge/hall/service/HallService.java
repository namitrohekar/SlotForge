package com.slotforge.hall.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.slotforge.audit.service.AuditService;
import com.slotforge.common.exception.ApiException;
import com.slotforge.common.persistence.QuerySupport;
import com.slotforge.common.response.PageResponse;
import com.slotforge.hall.dto.HallRequest;
import com.slotforge.hall.dto.HallResponse;
import com.slotforge.hall.entity.ExamHall;
import com.slotforge.hall.entity.HallStatus;
import com.slotforge.hall.repository.ExamHallRepository;
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
public class HallService {

    private static final Set<String> SORTABLE = Set.of("code", "name", "building", "capacity");

    private final ExamHallRepository halls;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<HallResponse> search(String q, HallStatus status, Pageable pageable) {
        QuerySupport.requireSortable(pageable, SORTABLE);
        Specification<ExamHall> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (StringUtils.hasText(q)) {
                String pattern = QuerySupport.containsPattern(q);
                where.add(cb.or(cb.like(cb.lower(root.get("code")), pattern, '\\'),
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("building")), pattern, '\\')));
            }
            if (status != null) where.add(cb.equal(root.get("status"), status));
            return cb.and(where.toArray(Predicate[]::new));
        };
        var page = halls.findAll(spec, pageable);
        Map<Long, Long> counts = QuerySupport.counts(page.getContent().stream().map(ExamHall::getId).toList(), halls::allocationCounts);
        return PageResponse.from(page.map(h -> HallResponse.from(h, counts.getOrDefault(h.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public HallResponse get(Long id) {
        return withCount(find(id));
    }

    @Transactional
    public HallResponse create(Long actorId, HallRequest request) {
        ExamHall hall = new ExamHall();
        apply(hall, request);
        halls.save(hall);
        audit.record(actorId, "HALL_CREATED", "exam_hall", hall.getId(), null);
        return HallResponse.from(hall, 0);
    }

    @Transactional
    public HallResponse update(Long actorId, Long id, HallRequest request) {
        ExamHall hall = find(id);
        apply(hall, request);
        audit.record(actorId, "HALL_UPDATED", "exam_hall", id, null);
        return withCount(hall);
    }

    @Transactional
    public void delete(Long actorId, Long id) {
        ExamHall hall = find(id);
        try {
            halls.delete(hall);
            halls.flush();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("IN_USE", hall.getCode() + " is used in a timetable. Mark it Retired instead.");
        }
        audit.record(actorId, "HALL_DELETED", "exam_hall", id, null);
    }

    private void apply(ExamHall hall, HallRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        long others = hall.getId() == null ? 0L : hall.getId();
        if (halls.existsByCodeIgnoreCaseAndIdNot(code, others)) {
            throw ApiException.conflict("DUPLICATE", "Another hall already uses the code " + code + ".");
        }
        hall.setCode(code);
        hall.setName(request.name().trim());
        hall.setBuilding(request.building().trim());
        hall.setFloor(request.floor());
        hall.setCapacity(request.capacity());
        hall.setStatus(request.status());
    }

    private HallResponse withCount(ExamHall hall) {
        return HallResponse.from(hall,
                QuerySupport.counts(List.of(hall.getId()), halls::allocationCounts).getOrDefault(hall.getId(), 0L));
    }

    private ExamHall find(Long id) {
        return halls.findById(id).orElseThrow(() -> ApiException.notFound("Hall", id));
    }
}
