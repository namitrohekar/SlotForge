package com.slotforge.planning.service;

import com.slotforge.common.exception.ApiException;
import com.slotforge.exam.entity.ExamSession;
import com.slotforge.exam.service.ExamSessionService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.slotforge.planning.algorithm.DsaturScheduler;
import com.slotforge.audit.service.AuditService;
import com.slotforge.planning.algorithm.GreedyScheduler;
import com.slotforge.planning.algorithm.SlotAdvisor;
import com.slotforge.planning.comparison.AlgorithmComparison;
import com.slotforge.planning.dto.BaselineResponse;
import com.slotforge.planning.dto.CandidateSlotResponse;
import com.slotforge.planning.dto.RepairResponse;
import com.slotforge.planning.dto.RunResponse;
import com.slotforge.planning.model.Constraint;
import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.HallSeats;
import com.slotforge.planning.model.Pins;
import com.slotforge.planning.model.PlanResult;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.SlotInfo;
import com.slotforge.planning.preflight.FeasibilityAnalyzer;
import com.slotforge.planning.repair.RepairPlanner;
import com.slotforge.schedule.service.BasePlanReader;
import com.slotforge.schedule.service.BasePlanReader.BasePlan;
import com.slotforge.schedule.service.PlanPersister;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * The run lifecycle. Solving holds no transaction. The draft is saved in one transaction,
 * so a failure at any point leaves the run marked FAILED and no partial timetable.
 */
@Service
@RequiredArgsConstructor
public class PlanningService {

    private final ExamSessionService sessions;
    private final ProblemLoader loader;
    private final PlanPersister persister;
    private final BasePlanReader bases;
    private final AuditService audit;
    private final JdbcClient jdbc;

    private static final Map<Constraint, String> WHY_NOT = Map.of(
            Constraint.SLOT_CLOSED, "that slot is blocked or already past",
            Constraint.DURATION, "that slot is too short for it",
            Constraint.WINDOW, "that slot is outside its allowed dates",
            Constraint.STUDENT_CLASH, "a student already has a paper then",
            Constraint.DAILY_LIMIT, "a student would be over the daily limit",
            Constraint.HEAVY_GAP, "it would be too close to a student's other heavy paper",
            Constraint.CAPACITY, "the halls free then cannot seat it",
            Constraint.STAFFING, "not enough invigilators are free then");

    /** Papers a coordinator placed by hand in the newest version keep their slot and halls. */
    public RunResponse generate(Long actorId, Long sessionId) {
        ExamSession session = sessions.requirePlanning(sessionId);
        ProblemLoader.Snapshot snapshot = loader.load(session);
        requireSomethingToSchedule(snapshot.problem());
        Optional<BasePlan> latest = bases.latest(sessionId);
        Map<Long, Long> locked = latest.map(BasePlan::lockedSlots).orElse(Map.of());
        Map<Long, List<HallSeats>> halls = new HashMap<>();
        latest.ifPresent(base -> locked.keySet().forEach(exam -> halls.put(exam, base.base().hallsByExam().getOrDefault(exam, List.of()))));

        return run(actorId, sessionId, "GENERATE", snapshot,
                problem -> DsaturScheduler.schedule(problem, new Pins(locked, halls, Map.of())),
                plan -> carry(null, locked, plan, Set.of(), Map.of(), null, "SCHEDULE_GENERATED"));
    }

    /**
     * A new draft from an existing timetable that changes as little as it can. Everything still valid keeps its slot,
     * halls and invigilators. The timetable it was made from is not touched, so a published one stays live meanwhile.
     */
    public RepairResponse repair(Long actorId, Long versionId) {
        BasePlan base = bases.read(versionId);
        if ("SUPERSEDED".equals(base.status())) {
            throw ApiException.conflict("VERSION_SUPERSEDED", "A newer timetable has replaced this one. Repair the newer one instead.");
        }
        return rebuild(actorId, base, base.lockedSlots(), "REPAIR", versionId, null, "SCHEDULE_REPAIRED");
    }

    /** Moves one paper of an unsent draft to a slot the rules allow, and keeps it there from now on. */
    public RepairResponse move(Long actorId, Long versionId, Long examId, Long slotId) {
        BasePlan base = editable(versionId);
        ExamSession session = sessions.requirePlanning(base.sessionId());
        SlotAdvisor.Candidate target = advise(loader.load(session).problem(), base, examId).stream()
                .filter(candidate -> candidate.slotId() == slotId)
                .findFirst().orElseThrow(() -> ApiException.notFound("Time slot", slotId));
        if (target.reason() != null) {
            throw ApiException.unprocessable("SLOT_NOT_ALLOWED", "This paper cannot move there: " + WHY_NOT.get(target.reason())
                    + (target.example() == null ? "" : " (" + target.example() + ")") + ".");
        }
        Map<Long, Long> mustStay = new HashMap<>(base.lockedSlots());
        mustStay.put(examId, slotId);
        return rebuild(actorId, base, mustStay, "REGENERATE", base.parentVersionId(), versionId, "SCHEDULE_PAPER_MOVED");
    }

    /** Every slot of the session for one paper, with the rule that keeps the paper out of it, if any. */
    public List<CandidateSlotResponse> candidateSlots(Long versionId, Long examId) {
        BasePlan base = bases.read(versionId);
        SchedulingProblem problem = loader.load(sessions.find(base.sessionId())).problem();
        Long current = base.base().slotByExam().get(examId);
        Map<Long, SlotInfo> slots = new HashMap<>();
        problem.slots().forEach(slot -> slots.put(slot.id(), slot));
        return advise(problem, base, examId).stream()
                .map(candidate -> {
                    SlotInfo slot = slots.get(candidate.slotId());
                    return new CandidateSlotResponse(slot.id(), slot.date(), slot.start(), slot.end(), slot.id() == current,
                            candidate.reason(), candidate.example());
                })
                .sorted(Comparator.comparing(CandidateSlotResponse::date).thenComparing(CandidateSlotResponse::start))
                .toList();
    }

    /** Keeps a paper where it is the next time the timetable is generated or repaired, or lets it go again. */
    public void setLocked(Long actorId, Long versionId, Long examId, boolean locked) {
        BasePlan base = editable(versionId);
        sessions.requirePlanning(base.sessionId());
        int changed = jdbc.sql("""
                UPDATE exam_schedule SET locked = :locked, source = CASE WHEN :locked THEN 'MANUAL' ELSE 'AUTO' END
                WHERE version_id = :version AND examination_id = :exam""")
                .param("locked", locked).param("version", versionId).param("exam", examId).update();
        if (changed == 0) throw ApiException.notFound("Paper", examId);
        audit.record(actorId, locked ? "SCHEDULE_PAPER_LOCKED" : "SCHEDULE_PAPER_UNLOCKED", "schedule_version", versionId, "{\"examinationId\":" + examId + "}");
    }

    private BasePlan editable(Long versionId) {
        BasePlan base = bases.read(versionId);
        if (!base.editable()) {
            throw ApiException.conflict("VERSION_NOT_EDITABLE",
                    "Only a draft that has not been sent for approval can be changed. Generate or repair to get a new draft.");
        }
        return base;
    }

    private List<SlotAdvisor.Candidate> advise(SchedulingProblem problem, BasePlan base, Long examId) {
        if (!base.base().slotByExam().containsKey(examId)) throw ApiException.notFound("Paper", examId);
        return SlotAdvisor.candidates(problem, base.base().slotByExam(), base.base().hallsByExam(), examId);
    }

    private RepairResponse rebuild(Long actorId, BasePlan base, Map<Long, Long> mustStay, String kind, Long parent, Long replaceDraft, String action) {
        ExamSession session = sessions.requirePlanning(base.sessionId());
        ProblemLoader.Snapshot snapshot = loader.load(session);
        requireSomethingToSchedule(snapshot.problem());
        RepairPlanner.Repair[] repaired = new RepairPlanner.Repair[1];
        RunResponse run = run(actorId, base.sessionId(), kind, snapshot,
                problem -> (repaired[0] = RepairPlanner.repair(problem, base.base(), mustStay)).plan(),
                plan -> carry(parent, mustStay, plan, repaired[0].moved(), base.invigilators(), replaceDraft, action));
        return new RepairResponse(run, repaired[0].level(), repaired[0].moved().size());
    }

    /** Papers still in the slot they had to keep stay locked. Anything else that moved is marked as moved by the repair. */
    private static PlanPersister.Carry carry(
            Long parent, Map<Long, Long> mustStay, PlanResult plan, Set<Long> moved, Map<String, List<Long>> invigilators, Long replaceDraft,
            String action) {
        Set<Long> locked = new HashSet<>();
        Map<Long, String> sources = new HashMap<>();
        moved.forEach(exam -> sources.put(exam, "REPAIR"));
        mustStay.forEach((exam, slot) -> {
            if (slot.equals(plan.slotByExam().get(exam))) {
                locked.add(exam);
                sources.put(exam, "MANUAL");
            }
        });
        return new PlanPersister.Carry(parent, locked, sources, invigilators, replaceDraft, action);
    }

    private RunResponse run(
            Long actorId, Long sessionId, String kind, ProblemLoader.Snapshot snapshot, Function<SchedulingProblem, PlanResult> solve,
            Function<PlanResult, PlanPersister.Carry> carry) {
        long runId = startRun(actorId, sessionId, kind);
        try {
            long started = System.nanoTime();
            PlanResult plan = solve.apply(snapshot.problem());
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            return persister.save(runId, sessionId, actorId, snapshot, plan, durationMs, carry.apply(plan));
        } catch (RuntimeException ex) {
            jdbc.sql("UPDATE scheduling_run SET status = 'FAILED', finished_at = now() WHERE id = :run").param("run", runId).update();
            throw ex;
        }
    }

    /** What is certain to go wrong if this session is generated as it stands. Reads only. */
    public List<FeasibilityAnalyzer.Finding> preflight(Long sessionId) {
        return FeasibilityAnalyzer.analyse(loader.load(sessions.find(sessionId)).problem());
    }

    /** DSATUR and first-fit on this session's current data, so the choice of algorithm is shown on real papers. Stores nothing. */
    public BaselineResponse baseline(Long sessionId) {
        SchedulingProblem problem = loader.load(sessions.find(sessionId)).problem();
        long students = problem.exams().stream().map(ExamNode::students).flatMap(java.util.Set::stream).distinct().count();
        return new BaselineResponse(problem.exams().size(), (int) students, (int) problem.slots().stream().filter(SlotInfo::open).count(),
                measured(DsaturScheduler::schedule, problem), measured(GreedyScheduler::schedule, problem));
    }

    private static AlgorithmComparison.Measures measured(Function<SchedulingProblem, PlanResult> algorithm, SchedulingProblem problem) {
        long started = System.nanoTime();
        PlanResult plan = algorithm.apply(problem);
        return AlgorithmComparison.measure(problem, plan, (System.nanoTime() - started) / 1_000_000.0);
    }

    public AlgorithmComparison.Report compareAlgorithms() {
        return AlgorithmComparison.run();
    }

    private static void requireSomethingToSchedule(SchedulingProblem problem) {
        if (problem.exams().isEmpty()) {
            throw ApiException.unprocessable("NOTHING_TO_SCHEDULE", "No paper has students yet. Add papers and enrol students first.");
        }
        if (problem.slots().stream().noneMatch(SlotInfo::open)) {
            throw ApiException.unprocessable("NO_OPEN_SLOTS", "This session has no open time slots left. Generate more slots first.");
        }
        if (problem.halls().isEmpty()) {
            throw ApiException.unprocessable("NO_HALLS", "No hall is available. Add a hall or mark one Available first.");
        }
    }

    private long startRun(Long actorId, Long sessionId, String kind) {
        try {
            return jdbc.sql("""
                    INSERT INTO scheduling_run (session_id, kind, algorithm, parameters, started_by)
                    VALUES (:session, :kind, 'DSATUR', '{}', :actor) RETURNING id""")
                    .param("session", sessionId).param("kind", kind).param("actor", actorId).query(Long.class).single();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("RUN_ALREADY_ACTIVE", "A timetable is already being generated for this session.");
        }
    }
}
