package com.voyage.global.idempotency;

import com.voyage.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A captured response for a completed idempotent POST, keyed per user. */
@Entity
@Table(name = "idempotency_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 512)
    private String path;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Lob
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    private IdempotencyRecord(Long userId, String idempotencyKey, String method, String path,
                             int statusCode, String responseBody) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.method = method;
        this.path = path;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public static IdempotencyRecord of(Long userId, String idempotencyKey, String method, String path,
                                       int statusCode, String responseBody) {
        return new IdempotencyRecord(userId, idempotencyKey, method, path, statusCode, responseBody);
    }
}
