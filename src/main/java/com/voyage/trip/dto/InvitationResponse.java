package com.voyage.trip.dto;

import com.voyage.trip.domain.Invitation;
import com.voyage.trip.domain.InvitationStatus;
import com.voyage.trip.domain.TripRole;
import java.time.Instant;

/** Invitation summary for the owner's pending-invitations list (no token). */
public record InvitationResponse(
        Long id,
        String email,
        TripRole role,
        InvitationStatus status,
        Instant expiresAt,
        Instant createdAt
) {

    public static InvitationResponse from(Invitation invitation) {
        return new InvitationResponse(
                invitation.getId(), invitation.getEmail(), invitation.getRole(),
                invitation.getStatus(), invitation.getExpiresAt(), invitation.getCreatedAt());
    }
}
