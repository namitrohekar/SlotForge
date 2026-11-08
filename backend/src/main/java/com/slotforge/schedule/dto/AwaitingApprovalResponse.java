package com.slotforge.schedule.dto;

import java.time.OffsetDateTime;

public record AwaitingApprovalResponse(
        long versionId, int versionNo, long sessionId, String sessionName, int papers, OffsetDateTime generatedAt) {
}
