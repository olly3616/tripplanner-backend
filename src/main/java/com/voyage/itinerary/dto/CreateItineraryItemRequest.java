package com.voyage.itinerary.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateItineraryItemRequest(
        @NotNull LocalDate date,
        Long placeId,
        LocalTime startsAt,
        LocalTime endsAt,
        @Size(max = 30) String transport,
        @Size(max = 1000) String note
) {
}
