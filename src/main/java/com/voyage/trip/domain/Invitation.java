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

/**
 * A trip invitation. Only the SHA-256 hash of the invite token is stored; the
 * raw token is returned to the inviter once and shared as a link.
 */
@Entity
@Table(name = "invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    /** Optional: the address the invite was intended for (null for shareable links). */
    private String email;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @Column(name = "invited_by")
    private Long invitedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    private Invitation(Long tripId, String email, String tokenHash, TripRole role,
                       Long invitedBy, Instant expiresAt) {
        this.tripId = tripId;
        this.email = email;
        this.tokenHash = tokenHash;
        this.role = role;
        this.status = InvitationStatus.PENDING;
        this.invitedBy = invitedBy;
        this.expiresAt = expiresAt;
    }

    public static Invitation create(Long tripId, String email, String tokenHash, TripRole role,
                                    Long invitedBy, Instant expiresAt) {
        return new Invitation(tripId, email, tokenHash, role, invitedBy, expiresAt);
    }

    public boolean isPending(Instant now) {
        return status == InvitationStatus.PENDING && expiresAt.isAfter(now);
    }

    public void accept(Instant now) {
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = now;
    }

    public void revoke() {
        this.status = InvitationStatus.REVOKED;
    }
}
