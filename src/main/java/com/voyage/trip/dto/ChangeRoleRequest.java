package com.voyage.trip.dto;

import com.voyage.trip.domain.TripRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull TripRole role
) {
}
