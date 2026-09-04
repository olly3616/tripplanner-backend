package com.voyage.trip.dto;

import com.voyage.trip.domain.TripRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Creates an invitation. {@code email} is optional (for shareable links). */
public record InviteRequest(
        @Email @Size(max = 255) String email,
        @NotNull TripRole role
) {
}
