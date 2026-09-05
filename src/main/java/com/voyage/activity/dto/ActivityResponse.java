package com.voyage.activity.dto;

import com.voyage.activity.domain.ActivityLog;
import java.time.Instant;

public record ActivityResponse(
        Long id,
        Long tripId,
        Long actorId,
        String action,
        String entityType,
        Long entityId,
        String message,
        Instant createdAt
) {

    public static ActivityResponse from(ActivityLog log) {
        return new ActivityResponse(
                log.getId(), log.getTripId(), log.getActorId(), log.getAction(),
                log.getEntityType(), log.getEntityId(), log.getMessage(), log.getCreatedAt());
    }
}
