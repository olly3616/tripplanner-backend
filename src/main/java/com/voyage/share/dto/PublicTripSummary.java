package com.voyage.share.dto;

import com.voyage.trip.domain.TripStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Read-only public view of a trip, exposed via a share link. */
public record PublicTripSummary(
        String title,
        String destination,
        LocalDate startsOn,
        LocalDate endsOn,
        String timezone,
        TripStatus status,
        List<ItineraryEntry> itinerary,
        List<PlaceEntry> places,
        Budget budget
) {

    public record ItineraryEntry(
            LocalDate date,
            LocalTime startsAt,
            LocalTime endsAt,
            String placeName,
            String note
    ) {
    }

    public record PlaceEntry(String name, String address, String category, String status) {
    }

    /** Present only when the link includes expenses. */
    public record Budget(String baseCurrency, long totalBaseMinor, List<CategoryTotal> categoryTotals) {
    }

    public record CategoryTotal(String category, long totalBaseMinor) {
    }
}
