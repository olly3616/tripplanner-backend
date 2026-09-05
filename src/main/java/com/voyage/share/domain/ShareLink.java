package com.voyage.share.domain;

import com.voyage.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "share_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShareLink extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "include_expenses", nullable = false)
    private boolean includeExpenses;

    private ShareLink(Long tripId, String tokenHash, String passwordHash,
                      Instant expiresAt, boolean includeExpenses) {
        this.tripId = tripId;
        this.tokenHash = tokenHash;
        this.passwordHash = passwordHash;
        this.expiresAt = expiresAt;
        this.includeExpenses = includeExpenses;
    }

    public static ShareLink create(Long tripId, String tokenHash, String passwordHash,
                                   Instant expiresAt, boolean includeExpenses) {
        return new ShareLink(tripId, tokenHash, passwordHash, expiresAt, includeExpenses);
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }
}
