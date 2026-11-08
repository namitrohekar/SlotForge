package com.slotforge.common.health;

public record HealthResponse(
        String status, String database, String postgresVersion, int schemaVersion, int requiredSchemaVersion) {
}
