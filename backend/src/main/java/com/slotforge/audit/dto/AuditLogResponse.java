package com.slotforge.audit.dto;

import java.time.OffsetDateTime;

/** actorName is null for an action whose account has since been removed. details is the stored JSON text, or null. */
public record AuditLogResponse(long id, OffsetDateTime at, String actorName, String action, String entityType, String entityId, String details) {
}
