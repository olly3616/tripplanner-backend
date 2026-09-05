package com.voyage.activity.domain;

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

/** A per-user in-app notification. */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "read_at")
    private Instant readAt;

    private Notification(Long userId, String type, Long tripId, String message) {
        this.userId = userId;
        this.type = type;
        this.tripId = tripId;
        this.message = message;
    }

    public static Notification of(Long userId, String type, Long tripId, String message) {
        return new Notification(userId, type, tripId, message);
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markRead(Instant now) {
        if (readAt == null) {
            this.readAt = now;
        }
    }
}
