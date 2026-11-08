package com.slotforge.exam.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.slotforge.audit.service.AuditService;
import com.slotforge.common.exception.ApiException;
import com.slotforge.common.persistence.QuerySupport;
import com.slotforge.exam.dto.GenerateSlotsRequest;
import com.slotforge.exam.dto.GenerateSlotsResponse;
import com.slotforge.exam.dto.SlotStatusRequest;
import com.slotforge.exam.dto.TimeSlotResponse;
import com.slotforge.exam.dto.TimeWindow;
import com.slotforge.exam.entity.ExamSession;
import com.slotforge.exam.entity.SlotStatus;
import com.slotforge.exam.entity.TimeSlot;
import com.slotforge.exam.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TimeSlotService {

    // Far beyond any real session: stops a mistyped date range from creating thousands of rows.
    private static final int MAX_SLOTS_PER_SESSION = 500;
    // The shortest paper a session can hold.
    private static final int MIN_SLOT_MINUTES = 15;
    private static final LocalTime EXAM_DAY_START = LocalTime.of(9, 30);
    private static final LocalTime EXAM_DAY_END = LocalTime.of(17, 0);

    private final TimeSlotRepository slots;
    private final ExamSessionService sessions;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> list(Long sessionId) {
        sessions.get(sessionId);
        List<TimeSlot> found = slots.findBySessionIdOrderBySlotDateAscStartTimeAsc(sessionId);
        Map<Long, Long> usage = QuerySupport.counts(found.stream().map(TimeSlot::getId).toList(), slots::usageCounts);
        return found.stream().map(slot -> TimeSlotResponse.from(slot, usage.getOrDefault(slot.getId(), 0L))).toList();
    }

    /** Safe to repeat: a time that overlaps an existing slot is skipped, never duplicated. */
    @Transactional
    public GenerateSlotsResponse generate(Long actorId, Long sessionId, GenerateSlotsRequest request) {
        ExamSession session = sessions.requirePlanning(sessionId);
        if (request.from().isAfter(request.to()) || request.from().isBefore(session.getStartDate())
                || request.to().isAfter(session.getEndDate())) {
            throw ApiException.unprocessable("OUTSIDE_SESSION", "Choose dates inside the session.");
        }
        if (request.from().isBefore(ExamSessionService.today())) {
            throw ApiException.unprocessable("PAST_DATE", "Slots cannot be created in the past.");
        }
        requireValidWindows(request.times());

        List<TimeSlot> taken = new ArrayList<>(slots.findBySessionIdOrderBySlotDateAscStartTimeAsc(sessionId));
        List<TimeSlot> created = new ArrayList<>();
        int skipped = 0;
        for (LocalDate date = request.from(); !date.isAfter(request.to()); date = date.plusDays(1)) {
            if (!request.weekdays().contains(date.getDayOfWeek().getValue())) continue;
            for (TimeWindow window : request.times()) {
                if (overlapsAny(taken, date, window)) {
                    skipped++;
                    continue;
                }
                TimeSlot slot = new TimeSlot();
                slot.setSession(session);
                slot.setSlotDate(date);
                slot.setStartTime(window.start());
                slot.setEndTime(window.end());
                taken.add(slot);
                created.add(slot);
            }
        }
        if (taken.size() > MAX_SLOTS_PER_SESSION) {
            throw ApiException.unprocessable("TOO_MANY_SLOTS", "A session can hold at most " + MAX_SLOTS_PER_SESSION + " slots.");
        }
        slots.saveAll(created);
        audit.record(actorId, "SLOTS_GENERATED", "exam_session", sessionId, "{\"created\":" + created.size() + "}");
        return new GenerateSlotsResponse(created.size(), skipped);
    }

    @Transactional
    public TimeSlotResponse changeStatus(Long actorId, Long id, SlotStatusRequest request) {
        TimeSlot slot = find(id);
        sessions.requirePlanning(slot.getSession().getId());
        boolean blocking = request.status() == SlotStatus.BLOCKED;
        if (blocking && !StringUtils.hasText(request.reason())) {
            throw ApiException.unprocessable("REASON_REQUIRED", "Say why this slot is blocked.");
        }
        slot.setStatus(request.status());
        slot.setBlockReason(blocking ? request.reason().trim() : null);
        audit.record(actorId, blocking ? "SLOT_BLOCKED" : "SLOT_OPENED", "time_slot", id, null);
        return TimeSlotResponse.from(slot, QuerySupport.counts(List.of(id), slots::usageCounts).getOrDefault(id, 0L));
    }

    @Transactional
    public void delete(Long actorId, Long id) {
        TimeSlot slot = find(id);
        sessions.requirePlanning(slot.getSession().getId());
        try {
            slots.delete(slot);
            slots.flush();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("IN_USE", "This slot is used in a timetable. Block it instead.");
        }
        audit.record(actorId, "SLOT_DELETED", "time_slot", id, null);
    }

    private static void requireValidWindows(List<TimeWindow> windows) {
        for (int index = 0; index < windows.size(); index++) {
            TimeWindow window = windows.get(index);
            if (window.start().isBefore(EXAM_DAY_START) || window.end().isAfter(EXAM_DAY_END)) {
                throw ApiException.unprocessable("OUTSIDE_EXAM_HOURS", "Exams run between " + EXAM_DAY_START + " and " + EXAM_DAY_END + ".");
            }
            if (Duration.between(window.start(), window.end()).toMinutes() < MIN_SLOT_MINUTES) {
                throw ApiException.unprocessable("INVALID_TIME_WINDOW", "A slot must end after it starts and last at least " + MIN_SLOT_MINUTES + " minutes.");
            }
            for (TimeWindow other : windows.subList(index + 1, windows.size())) {
                if (window.start().isBefore(other.end()) && other.start().isBefore(window.end())) {
                    throw ApiException.unprocessable("INVALID_TIME_WINDOW", "The times overlap each other.");
                }
            }
        }
    }

    private static boolean overlapsAny(List<TimeSlot> taken, LocalDate date, TimeWindow window) {
        return taken.stream().anyMatch(slot -> slot.getSlotDate().equals(date)
                && window.start().isBefore(slot.getEndTime()) && slot.getStartTime().isBefore(window.end()));
    }

    private TimeSlot find(Long id) {
        return slots.findById(id).orElseThrow(() -> ApiException.notFound("Time slot", id));
    }
}
