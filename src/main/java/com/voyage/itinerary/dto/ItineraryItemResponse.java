package com.voyage.itinerary.dto;

import com.voyage.itinerary.domain.ItineraryItem;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record ItineraryItemResponse(
        Long id,
        Long tripId,
        Long placeId,
        LocalDate date,
        LocalTime startsAt,
        LocalTime endsAt,
        int sortOrder,
        String transport,
        String note,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {

    public static ItineraryItemResponse from(ItineraryItem item) {
        return new ItineraryItemResponse(
                item.getId(), item.getTripId(), item.getPlaceId(), item.getDate(),
                item.getStartsAt(), item.getEndsAt(), item.getSortOrder(), item.getTransport(),
                item.getNote(), item.getVersion(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
