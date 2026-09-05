package com.voyage.activity.domain;

import com.voyage.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Append-only record of who changed what, shown as the trip's activity feed. */
@Entity
@Table(name = "activity_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false, length = 500)
    private String message;

    private ActivityLog(Long tripId, Long actorId, String action, String entityType,
                        Long entityId, String message) {
        this.tripId = tripId;
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.message = message;
    }

    public static ActivityLog of(Long tripId, Long actorId, String action, String entityType,
                                 Long entityId, String message) {
        return new ActivityLog(tripId, actorId, action, entityType, entityId, message);
    }
}
