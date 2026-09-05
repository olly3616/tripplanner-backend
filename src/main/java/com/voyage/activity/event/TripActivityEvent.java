package com.voyage.activity.event;

/**
 * Published by domain services when something noteworthy happens in a trip.
 * A listener turns it into an activity-log entry and per-member notifications.
 *
 * @param title short human label of the affected entity (e.g. the expense title)
 */
public record TripActivityEvent(
        Long tripId,
        Long actorId,
        String action,
        String entityType,
        Long entityId,
        String title
) {

    public static final String EXPENSE_CREATED = "EXPENSE_CREATED";
    public static final String POLL_CREATED = "POLL_CREATED";
}
