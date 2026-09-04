package com.voyage.itinerary.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Partial update. {@code version} is the value the client last saw; a mismatch
 * means someone else changed the item first and the request is rejected with 409.
 * Other {@code null} fields are left unchanged.
 */
public record UpdateItineraryItemRequest(
        @NotNull Long version,
        LocalDate date,
        Long placeId,
        LocalTime startsAt,
        LocalTime endsAt,
        @Size(max = 30) String transport,
        @Size(max = 1000) String note
) {
}
