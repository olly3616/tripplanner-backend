package com.voyage.place.dto;

import com.voyage.place.domain.PlaceStatus;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Partial update. {@code null} fields are left unchanged; {@code tags} replaces the whole list. */
public record UpdatePlaceRequest(
        @Size(max = 200) String name,
        @Size(max = 500) String address,
        @Size(max = 100) String category,
        PlaceStatus status,
        List<@Size(max = 50) String> tags,
        @Size(max = 1000) String note
) {
}
