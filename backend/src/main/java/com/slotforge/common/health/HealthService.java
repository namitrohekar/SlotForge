package com.slotforge.common.health;

import com.slotforge.common.config.SlotForgeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthService {

    private final JdbcClient jdbc;
    private final SlotForgeProperties properties;

    public HealthResponse read() {
        return jdbc.sql("""
                        SELECT current_database() AS database_name,
                               current_setting('server_version') AS postgres_version,
                               (SELECT max(version) FROM schema_version) AS schema_version
                        """)
                .query((row, rowNumber) -> new HealthResponse(
                        "UP",
                        row.getString("database_name"),
                        row.getString("postgres_version"),
                        row.getInt("schema_version"),
                        properties.schema().requiredVersion()))
                .single();
    }
}
