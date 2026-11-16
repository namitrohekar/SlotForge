package com.slotforge.audit.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import com.slotforge.audit.dto.AuditLogResponse;
import com.slotforge.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads the audit trail, newest first. It is never changed through the application. */
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private static final int MAX_SIZE = 100;
    // Actions and entities are stored with underscores and names with spaces, so a name is compared with its spaces turned into underscores.
    private static final String FILTER = """
            FROM audit_log a LEFT JOIN app_user u ON u.id = a.actor_user_id
            WHERE :q = '' OR lower(a.action) LIKE :like OR lower(a.entity_type) LIKE :like
               OR lower(replace(u.full_name, ' ', '_')) LIKE :like""";

    private final JdbcClient jdbc;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String q, int page, int size) {
        String term = q == null ? "" : q.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        String like = "%" + term.replace("%", "") + "%";
        int limit = Math.max(1, Math.min(size, MAX_SIZE));
        int offset = Math.max(0, page) * limit;
        long total = jdbc.sql("SELECT count(*) " + FILTER).param("q", term).param("like", like).query(Long.class).single();
        List<AuditLogResponse> items = jdbc.sql("SELECT a.id, a.created_at, u.full_name, a.action, a.entity_type, a.entity_id, a.details "
                        + FILTER + " ORDER BY a.created_at DESC, a.id DESC LIMIT :limit OFFSET :offset")
                .param("q", term).param("like", like).param("limit", limit).param("offset", offset)
                .query((row, index) -> new AuditLogResponse(row.getLong(1), row.getObject(2, OffsetDateTime.class), row.getString(3),
                        row.getString(4), row.getString(5), row.getString(6), row.getString(7)))
                .list();
        return new PageResponse<>(items, Math.max(0, page), limit, total, (int) ((total + limit - 1) / limit));
    }
}
