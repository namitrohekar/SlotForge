package com.slotforge.audit.service;

import com.slotforge.audit.entity.AuditLog;
import com.slotforge.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository logs;

    /** details must be a JSON string or null. The database rejects anything else. */
    @Transactional
    public void record(Long actorUserId, String action, String entityType, Object entityId, String details) {
        logs.save(new AuditLog(actorUserId, action, entityType, entityId == null ? null : entityId.toString(), details));
    }
}
