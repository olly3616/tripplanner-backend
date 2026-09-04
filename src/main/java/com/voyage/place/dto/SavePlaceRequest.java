package com.voyage.place.dto;

import com.voyage.place.domain.PlaceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Saves a place (from a search result or entered manually). */
public record SavePlaceRequest(
        @Size(max = 30) String provider,
        @Size(max = 255) String providerPlaceId,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String address,
        Double latitude,
        Double longitude,
        @Size(max = 100) String category,
        PlaceStatus status,
        List<@Size(max = 50) String> tags,
        @Size(max = 1000) String note
) {
}
