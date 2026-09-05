package com.voyage.ai.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** An editable, non-committed day-by-day itinerary suggestion. */
public record ItineraryDraftResponse(List<DraftDay> days) {

    public record DraftDay(LocalDate date, List<DraftItem> items) {
    }

    public record DraftItem(Long placeId, String placeName, LocalTime startsAt, String note) {
    }
}
