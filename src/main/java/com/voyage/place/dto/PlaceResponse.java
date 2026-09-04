package com.voyage.place.dto;

import com.voyage.place.domain.PlaceStatus;
import com.voyage.place.domain.SavedPlace;
import java.time.Instant;
import java.util.List;

public record PlaceResponse(
        Long id,
        Long tripId,
        String provider,
        String providerPlaceId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        String category,
        PlaceStatus status,
        List<String> tags,
        String note,
        Instant createdAt,
        Instant updatedAt
) {

    public static PlaceResponse from(SavedPlace place) {
        return new PlaceResponse(
                place.getId(), place.getTripId(), place.getProvider(), place.getProviderPlaceId(),
                place.getName(), place.getAddress(), place.getLatitude(), place.getLongitude(),
                place.getCategory(), place.getStatus(),
                place.getTags() != null ? place.getTags() : List.of(),
                place.getNote(), place.getCreatedAt(), place.getUpdatedAt());
    }
}
