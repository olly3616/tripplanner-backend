package com.voyage.trip.dto;

import com.voyage.trip.domain.MemberStatus;
import com.voyage.trip.domain.TripMember;
import com.voyage.trip.domain.TripRole;
import com.voyage.user.domain.User;
import java.time.Instant;

public record MemberResponse(
        Long userId,
        String name,
        String email,
        String avatarUrl,
        TripRole role,
        MemberStatus status,
        Instant joinedAt
) {

    public static MemberResponse of(TripMember member, User user) {
        return new MemberResponse(
                user.getId(), user.getName(), user.getEmail(), user.getAvatarUrl(),
                member.getRole(), member.getStatus(), member.getJoinedAt());
    }
}
