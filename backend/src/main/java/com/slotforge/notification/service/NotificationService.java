package com.slotforge.notification.service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import com.slotforge.notification.dto.NotificationsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Notices people see inside the app. Everything read here is scoped to the signed-in person. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int SHOWN = 30;

    private final JdbcClient jdbc;

    /** One notice for each person. Joins the caller's transaction, so a rolled-back action tells nobody. */
    public void notify(Collection<Long> userIds, String kind, String title, String body, String link) {
        for (Long user : new LinkedHashSet<>(userIds)) {
            jdbc.sql("INSERT INTO notification (user_id, kind, title, body, link) VALUES (:user, :kind, :title, :body, :link)")
                    .param("user", user).param("kind", kind).param("title", shorten(title, 200)).param("body", shorten(body, 500)).param("link", link)
                    .update();
        }
    }

    @Transactional(readOnly = true)
    public NotificationsResponse list(Long userId) {
        int unread = jdbc.sql("SELECT count(*) FROM notification WHERE user_id = :user AND read_at IS NULL")
                .param("user", userId).query(Integer.class).single();
        List<NotificationsResponse.Item> items = jdbc.sql("""
                SELECT id, kind, title, body, link, created_at, read_at IS NOT NULL
                FROM notification WHERE user_id = :user ORDER BY created_at DESC, id DESC LIMIT :shown""")
                .param("user", userId).param("shown", SHOWN)
                .query((row, index) -> new NotificationsResponse.Item(row.getLong(1), row.getString(2), row.getString(3), row.getString(4),
                        row.getString(5), row.getObject(6, OffsetDateTime.class), row.getBoolean(7)))
                .list();
        return new NotificationsResponse(unread, items);
    }

    @Transactional
    public void markAllRead(Long userId) {
        jdbc.sql("UPDATE notification SET read_at = now() WHERE user_id = :user AND read_at IS NULL").param("user", userId).update();
    }

    private static String shorten(String text, int limit) {
        return text == null || text.length() <= limit ? text : text.substring(0, limit - 1) + "…";
    }
}
