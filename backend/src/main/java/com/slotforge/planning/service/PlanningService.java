package com.slotforge.planning.service;

import com.slotforge.common.exception.ApiException;
import com.slotforge.exam.entity.ExamSession;
import com.slotforge.exam.service.ExamSessionService;
import com.slotforge.planning.algorithm.DsaturScheduler;
import com.slotforge.planning.dto.RunResponse;
import com.slotforge.planning.model.PlanResult;
import com.slotforge.planning.model.SchedulingProblem;
import com.slotforge.planning.model.SlotInfo;
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

    private static void requireSomethingToSchedule(SchedulingProblem problem) {
        if (problem.exams().isEmpty()) {
            throw ApiException.unprocessable("NOTHING_TO_SCHEDULE", "No paper has students yet. Add papers and enrol students first.");
        }
        if (problem.slots().stream().noneMatch(SlotInfo::open)) {
            throw ApiException.unprocessable("NO_OPEN_SLOTS", "This session has no open time slots. Generate slots first.");
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
