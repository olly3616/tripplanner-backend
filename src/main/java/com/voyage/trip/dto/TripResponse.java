package com.voyage.trip.dto;

import com.voyage.trip.domain.Trip;
import com.voyage.trip.domain.TripRole;
import com.voyage.trip.domain.TripStatus;
import java.time.Instant;
import java.time.LocalDate;

public record TripResponse(
        Long id,
        Long ownerId,
        String title,
        String destination,
        LocalDate startsOn,
        LocalDate endsOn,
        String baseCurrency,
        String timezone,
        TripStatus status,
        String coverImageUrl,
        TripRole myRole,
        int memberCount,
        Instant createdAt,
        Instant updatedAt
) {

    public static TripResponse from(Trip trip, TripRole myRole, int memberCount) {
        return new TripResponse(
                trip.getId(),
                trip.getOwnerId(),
                trip.getTitle(),
                trip.getDestination(),
                trip.getStartsOn(),
                trip.getEndsOn(),
                trip.getBaseCurrency(),
                trip.getTimezone(),
                trip.getStatus(),
                trip.getCoverImageUrl(),
                myRole,
                memberCount,
                trip.getCreatedAt(),
                trip.getUpdatedAt()
        );
    }
}
