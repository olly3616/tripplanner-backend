package com.voyage.activity.dto;

import com.voyage.activity.domain.Notification;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        Long tripId,
        String message,
        Instant readAt,
        boolean read,
        Instant createdAt
) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTripId(), n.getMessage(),
                n.getReadAt(), n.isRead(), n.getCreatedAt());
    }
}
