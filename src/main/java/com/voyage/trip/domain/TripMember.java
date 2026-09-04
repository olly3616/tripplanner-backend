package com.voyage.trip.domain;

import com.voyage.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trip_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    private TripMember(Long tripId, Long userId, TripRole role) {
        this.tripId = tripId;
        this.userId = userId;
        this.role = role;
        this.status = MemberStatus.ACTIVE;
        this.joinedAt = Instant.now();
    }

    public static TripMember owner(Long tripId, Long userId) {
        return new TripMember(tripId, userId, TripRole.OWNER);
    }

    public static TripMember member(Long tripId, Long userId, TripRole role) {
        return new TripMember(tripId, userId, role);
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public boolean isOwner() {
        return role == TripRole.OWNER;
    }

    public void changeRole(TripRole role) {
        this.role = role;
    }

    public void remove() {
        this.status = MemberStatus.REMOVED;
    }

    /** Re-activates a previously removed membership (e.g. re-invited), refreshing role and join time. */
    public void reactivate(TripRole role) {
        this.role = role;
        this.status = MemberStatus.ACTIVE;
        this.joinedAt = Instant.now();
    }
}
