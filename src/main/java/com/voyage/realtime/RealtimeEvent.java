package com.voyage.realtime;

import com.voyage.activity.event.TripActivityEvent;

/** Payload pushed to a trip's subscribers over WebSocket. */
public record RealtimeEvent(
        String action,
        Long tripId,
        String entityType,
        Long entityId,
        Long actorId,
        String title
) {

    public static RealtimeEvent from(TripActivityEvent event) {
        return new RealtimeEvent(event.action(), event.tripId(), event.entityType(),
                event.entityId(), event.actorId(), event.title());
    }
}
