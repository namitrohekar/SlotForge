package com.slotforge.common.config;

import com.slotforge.common.health.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Read-only. Stops startup when the database is behind the code. A newer database is fine. */
@Component
@RequiredArgsConstructor
public class SchemaVersionCheck implements ApplicationRunner {

    private final HealthService health;
    private final SlotForgeProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        int actual = health.read().schemaVersion();
        int required = properties.schema().requiredVersion();
        if (actual < required) {
            throw new IllegalStateException(
                    "Database is at schema version %d but this code needs %d. Run database/migrations/V%03d onward in pgAdmin."
                            .formatted(actual, required, actual + 1));
        }
    }
}
