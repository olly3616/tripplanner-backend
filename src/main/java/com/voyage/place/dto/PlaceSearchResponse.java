package com.voyage.place.dto;

import com.voyage.place.search.PlaceSearchResult;

public record PlaceSearchResponse(
        String provider,
        String providerPlaceId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        String category
) {

    public static PlaceSearchResponse from(PlaceSearchResult result) {
        return new PlaceSearchResponse(
                result.provider(), result.providerPlaceId(), result.name(), result.address(),
                result.latitude(), result.longitude(), result.category());
    }
}
