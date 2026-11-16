package com.slotforge.notification.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** The newest notices for the signed-in person. link is a path inside the app, or null. */
public record NotificationsResponse(int unread, List<Item> items) {

    public record Item(long id, String kind, String title, String body, String link, OffsetDateTime createdAt, boolean read) {
    }
}
