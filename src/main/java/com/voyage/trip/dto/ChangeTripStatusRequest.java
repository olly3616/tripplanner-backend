package com.voyage.trip.dto;

import com.voyage.trip.domain.TripStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTripStatusRequest(
        @NotNull TripStatus status
) {
}
