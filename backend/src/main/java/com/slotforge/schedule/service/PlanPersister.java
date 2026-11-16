package com.slotforge.schedule.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.slotforge.audit.service.AuditService;
import com.slotforge.exam.service.ExamSessionService;
import com.slotforge.planning.dto.RunReport;
import com.slotforge.planning.allocation.InvigilatorAllocator;
import com.slotforge.planning.dto.RunResponse;
import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallInfo;
import com.slotforge.planning.model.HallSeats;
import com.slotforge.planning.model.PlanResult;
import com.slotforge.planning.model.SlotInfo;
import com.slotforge.planning.quality.QualityScorer;
import com.slotforge.planning.quality.QualityScorer.Quality;
import com.slotforge.planning.service.ProblemLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Writes one plan as one DRAFT, all or nothing. */
@Service
@RequiredArgsConstructor
public class PlanPersister {

    private final JdbcClient jdbc;
    private final JdbcTemplate batch;
    private final JsonMapper json;
    private final AuditService audit;
    private final ExamSessionService sessions;

    @Transactional
    public RunResponse save(long runId, long sessionId, long actorId, ProblemLoader.Snapshot snapshot, PlanResult plan, long durationMs) {
        // Solving runs outside any transaction, so the session may have been cancelled in the meantime.
        sessions.requirePlanning(sessionId);
        int total = snapshot.problem().exams().size();
        int placed = plan.slotByExam().size();
        int slotsUsed = new HashSet<>(plan.slotByExam().values()).size();
        String status = placed == 0 ? "FAILED" : plan.unplaced().isEmpty() ? "SUCCEEDED" : "PARTIAL";

        Long versionId = null;
        Integer versionNo = null;
        Quality quality = null;
        if (placed > 0) {
            // Counted from runs, which outlive a deleted draft, so a number never comes back.
            versionNo = jdbc.sql("SELECT count(*) + 1 FROM scheduling_run WHERE session_id = :session AND status IN ('SUCCEEDED', 'PARTIAL')")
                    .param("session", sessionId).query(Integer.class).single();
            versionId = jdbc.sql("""
                    INSERT INTO schedule_version (session_id, version_no, source_run_id, created_by)
                    VALUES (:session, :number, :run, :actor) RETURNING id""")
                    .param("session", sessionId).param("number", versionNo).param("run", runId).param("actor", actorId)
                    .query(Long.class).single();
            quality = QualityScorer.score(snapshot.problem(), plan, writeEntries(versionId, snapshot, plan));
            jdbc.sql("UPDATE schedule_version SET quality_score = :score WHERE id = :version")
                    .param("score", quality.score()).param("version", versionId).update();
            audit.record(actorId, "SCHEDULE_GENERATED", "schedule_version", versionId, "{\"placed\":" + placed + ",\"total\":" + total + "}");
        }

        jdbc.sql("""
                UPDATE scheduling_run
                SET status = :status, report = :report, exams_total = :total, exams_scheduled = :placed, slots_used = :slots,
                    search_nodes = :nodes, backtracks = :backtracks, duration_ms = :duration, finished_at = now()
                WHERE id = :run""")
                .param("status", status).param("report", json.writeValueAsString(new RunReport(plan.unplaced(), plan.trace(), quality)))
                .param("total", total).param("placed", placed).param("slots", slotsUsed)
                .param("nodes", plan.searchNodes()).param("backtracks", plan.backtracks()).param("duration", durationMs)
                .param("run", runId).update();

        return new RunResponse(runId, status, versionId, versionNo, total, placed, slotsUsed, plan.searchNodes(), plan.backtracks(),
                durationMs, plan.unplaced());
    }

    /** Returns how many duties each person in the pool was given, zeros included. */
    private Collection<Integer> writeEntries(long versionId, ProblemLoader.Snapshot snapshot, PlanResult plan) {
        Map<Long, Integer> capacity = snapshot.problem().halls().stream().collect(Collectors.toMap(HallInfo::id, HallInfo::capacity));
        List<Object[]> seats = new ArrayList<>();
        List<InvigilatorAllocator.Duty> duties = new ArrayList<>();
        for (ExamNode exam : snapshot.problem().exams()) {
            Long slotId = plan.slotByExam().get(exam.id());
            if (slotId == null) continue;
            long scheduleId = jdbc.sql("""
                    INSERT INTO exam_schedule (version_id, examination_id, time_slot_id)
                    VALUES (:version, :exam, :slot) RETURNING id""")
                    .param("version", versionId).param("exam", exam.id()).param("slot", slotId).query(Long.class).single();

            List<Long> roster = snapshot.rosters().get(exam.id());
            int seated = 0;
            for (HallSeats hall : plan.hallsByExam().get(exam.id())) {
                long allocationId = jdbc.sql("""
                        INSERT INTO hall_allocation (exam_schedule_id, version_id, time_slot_id, hall_id, seats_allocated, capacity_snapshot)
                        VALUES (:schedule, :version, :slot, :hall, :seats, :capacity) RETURNING id""")
                        .param("schedule", scheduleId).param("version", versionId).param("slot", slotId).param("hall", hall.hallId())
                        .param("seats", hall.seats()).param("capacity", capacity.get(hall.hallId()))
                        .query(Long.class).single();
                for (long student : roster.subList(seated, seated + hall.seats())) {
                    seats.add(new Object[] {scheduleId, exam.id(), allocationId, student});
                }
                seated += hall.seats();
                duties.add(new InvigilatorAllocator.Duty(allocationId, slotId, InvigilatorAllocator.needed(hall.seats(), snapshot.studentsPerInvigilator())));
            }
        }
        batch.batchUpdate("""
                INSERT INTO student_hall_assignment (exam_schedule_id, examination_id, hall_allocation_id, student_id)
                VALUES (?, ?, ?, ?)""", seats);
        return writeInvigilators(versionId, snapshot, duties);
    }

    private Collection<Integer> writeInvigilators(long versionId, ProblemLoader.Snapshot snapshot, List<InvigilatorAllocator.Duty> duties) {
        Map<Long, SlotInfo> slots = snapshot.problem().slots().stream().collect(Collectors.toMap(SlotInfo::id, Function.identity()));
        List<InvigilatorAllocator.Duty> inOrder = duties.stream()
                .sorted(Comparator.comparing((InvigilatorAllocator.Duty duty) -> slots.get(duty.slotId()).date())
                        .thenComparing(duty -> slots.get(duty.slotId()).start())
                        .thenComparingLong(InvigilatorAllocator.Duty::key))
                .toList();
        Map<Long, List<Long>> assigned = InvigilatorAllocator.allocate(inOrder, snapshot.invigilators(), snapshot.maxDuties());
        List<Object[]> rows = new ArrayList<>();
        for (InvigilatorAllocator.Duty duty : inOrder) {
            for (long person : assigned.get(duty.key())) {
                rows.add(new Object[] {duty.key(), versionId, duty.slotId(), person});
            }
        }
        batch.batchUpdate("""
                INSERT INTO invigilator_assignment (hall_allocation_id, version_id, time_slot_id, user_id)
                VALUES (?, ?, ?, ?)""", rows);
        Map<Long, Integer> load = new HashMap<>();
        snapshot.invigilators().forEach(person -> load.put(person.id(), 0));
        rows.forEach(row -> load.merge((Long) row[3], 1, Integer::sum));
        return load.values();
    }
}
