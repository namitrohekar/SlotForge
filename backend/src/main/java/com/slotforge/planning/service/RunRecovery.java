package com.slotforge.planning.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** A crash mid-run would otherwise leave a RUNNING row that blocks that session forever. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RunRecovery implements ApplicationRunner {

    private final JdbcClient jdbc;

    @Override
    public void run(ApplicationArguments args) {
        int closed = jdbc.sql("UPDATE scheduling_run SET status = 'FAILED', finished_at = now() WHERE status = 'RUNNING'").update();
        if (closed > 0) log.warn("Closed {} scheduling run(s) interrupted by a restart", closed);
    }
}
