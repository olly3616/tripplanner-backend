package com.voyage.trip.dto;

import com.voyage.trip.domain.TripRole;

public record AcceptInvitationResponse(
        Long tripId,
        TripRole role
) {
}
