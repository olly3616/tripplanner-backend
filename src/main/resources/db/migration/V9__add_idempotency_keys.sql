-- V9: stored responses for idempotent POSTs, so an offline client's retried
-- mutation (same Idempotency-Key) returns the original result instead of
-- creating a duplicate. Scoped per user.

CREATE TABLE idempotency_keys
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    method          VARCHAR(10)  NOT NULL,
    path            VARCHAR(512) NOT NULL,
    status_code     INT          NOT NULL,
    response_body   TEXT         NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_idem_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_idem_user_key UNIQUE (user_id, idempotency_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
