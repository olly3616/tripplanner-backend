package com.voyage.trip.service;

import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.trip.domain.MemberStatus;
import com.voyage.trip.domain.TripMember;
import com.voyage.trip.domain.TripRole;
import com.voyage.trip.repository.TripMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for per-trip authorization. Every trip sub-resource
 * (itinerary, places, expenses, ...) resolves access through this guard.
 *
 * <p>Policy: a non-member sees {@code 404} (existence is hidden), while a member
 * lacking the required role gets {@code 403}.
 */
@Component
@RequiredArgsConstructor
public class TripAccessGuard {

    private final TripMemberRepository tripMemberRepository;

    /** Requires an active membership; hides the trip from non-members (404). */
    public TripMember requireActiveMember(Long tripId, Long userId) {
        return tripMemberRepository
                .findByTripIdAndUserIdAndStatus(tripId, userId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /** Requires the caller to be the trip owner (403 if member but not owner). */
    public TripMember requireOwner(Long tripId, Long userId) {
        TripMember membership = requireActiveMember(tripId, userId);
        if (membership.getRole() != TripRole.OWNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return membership;
    }

    /** Requires the caller to hold one of the allowed roles (403 otherwise). */
    public TripMember requireAnyRole(Long tripId, Long userId, TripRole... allowedRoles) {
        TripMember membership = requireActiveMember(tripId, userId);
        for (TripRole role : allowedRoles) {
            if (membership.getRole() == role) {
                return membership;
            }
        }
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
}
