package com.slotforge.planning.service;

import com.slotforge.common.exception.ApiException;
import com.slotforge.exam.entity.ExamSession;
import com.slotforge.exam.service.ExamSessionService;
import java.util.List;
import java.util.function.Function;

import com.slotforge.planning.algorithm.DsaturScheduler;
import com.slotforge.planning.algorithm.GreedyScheduler;
import com.slotforge.planning.comparison.AlgorithmComparison;
import com.slotforge.planning.dto.BaselineResponse;
import com.slotforge.planning.dto.RunResponse;
import com.slotforge.planning.model.ExamNode;
import com.slotforge.planning.model.PlanResult;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.SlotInfo;
import com.slotforge.planning.preflight.FeasibilityAnalyzer;
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
    private final JdbcClient jdbc;

    public RunResponse generate(Long actorId, Long sessionId) {
        ExamSession session = sessions.requirePlanning(sessionId);
        ProblemLoader.Snapshot snapshot = loader.load(session);
        requireSomethingToSchedule(snapshot.problem());

        long runId = startRun(actorId, sessionId);
        try {
            long started = System.nanoTime();
            PlanResult plan = DsaturScheduler.schedule(snapshot.problem());
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            return persister.save(runId, sessionId, actorId, snapshot, plan, durationMs);
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

    private long startRun(Long actorId, Long sessionId) {
        try {
            return jdbc.sql("""
                    INSERT INTO scheduling_run (session_id, kind, algorithm, parameters, started_by)
                    VALUES (:session, 'GENERATE', 'DSATUR', '{}', :actor) RETURNING id""")
                    .param("session", sessionId).param("actor", actorId).query(Long.class).single();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("RUN_ALREADY_ACTIVE", "A timetable is already being generated for this session.");
        }
    }
}
