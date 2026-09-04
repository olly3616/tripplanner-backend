package com.voyage.trip.dto;

import com.voyage.trip.domain.Invitation;
import com.voyage.trip.domain.TripRole;
import java.time.Instant;

/**
 * Returned once when an invitation is created. The raw {@code token} is only
 * ever exposed here; the client builds the invite link from it.
 */
public record InviteResponse(
        Long invitationId,
        String token,
        TripRole role,
        String email,
        Instant expiresAt
) {

    public static InviteResponse of(Invitation invitation, String rawToken) {
        return new InviteResponse(
                invitation.getId(), rawToken, invitation.getRole(),
                invitation.getEmail(), invitation.getExpiresAt());
    }
}
