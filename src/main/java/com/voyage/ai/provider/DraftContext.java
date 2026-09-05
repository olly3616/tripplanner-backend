package com.voyage.ai.provider;

import java.time.LocalDate;
import java.util.List;

public record DraftContext(
        LocalDate startsOn,
        LocalDate endsOn,
        List<PlaceCandidate> places,
        int itemsPerDay,
        List<String> preferredCategories
) {

    public record PlaceCandidate(Long placeId, String name, String category, String status) {
    }
}
