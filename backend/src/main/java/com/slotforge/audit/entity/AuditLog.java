package com.slotforge.audit.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long actorUserId;

    private String action;

    private String entityType;

    private String entityId;

    // TEXT with an IS JSON check, because Hibernate 7.2 has no Jackson 3 JSON mapper.
    private String details;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    public AuditLog(Long actorUserId, String action, String entityType, String entityId, String details) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }
}
