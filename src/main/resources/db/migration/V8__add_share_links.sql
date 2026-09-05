-- V8: password-optional, read-only public share links for a trip.
-- Only the hash of the share token is stored; password (if any) is BCrypt-hashed.

CREATE TABLE share_links
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    trip_id          BIGINT       NOT NULL,
    token_hash       VARCHAR(255) NOT NULL,
    password_hash    VARCHAR(255) NULL,
    expires_at       DATETIME(6)  NULL,
    include_expenses BIT          NOT NULL DEFAULT 0,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_share_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    CONSTRAINT uq_share_token UNIQUE (token_hash)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_share_trip ON share_links (trip_id);
