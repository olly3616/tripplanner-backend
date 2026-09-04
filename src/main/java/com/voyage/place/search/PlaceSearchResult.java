package com.voyage.place.search;

/** A place returned by an external provider search (not yet saved). */
public record PlaceSearchResult(
        String provider,
        String providerPlaceId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        String category
) {
}
