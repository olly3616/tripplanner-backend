-- V1: Core tables for milestone 1 (auth, trips, membership, invitations).
-- Conventions:
--   * InnoDB + utf8mb4 for full Unicode (emoji-safe) support.
--   * Timestamps are DATETIME(6) storing UTC (see spring.jpa hibernate.jdbc.time_zone=UTC).
--   * Enum-like columns use VARCHAR + CHECK constraints (flexible for migrations,
--     while MySQL 8 still enforces valid values).

CREATE TABLE users
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    email            VARCHAR(255) NOT NULL,
    password_hash    VARCHAR(255) NULL,                     -- NULL for social-only accounts
    name             VARCHAR(100) NOT NULL,
    avatar_url       VARCHAR(512) NULL,
    default_currency CHAR(3)      NOT NULL DEFAULT 'KRW',
    timezone         VARCHAR(64)  NOT NULL DEFAULT 'Asia/Seoul',
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE trips
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id        BIGINT       NOT NULL,
    title           VARCHAR(150) NOT NULL,
    destination     VARCHAR(150) NULL,
    starts_on       DATE         NOT NULL,
    ends_on         DATE         NOT NULL,
    base_currency   CHAR(3)      NOT NULL,
    timezone        VARCHAR(64)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    cover_image_url VARCHAR(512) NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_trips_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_trips_status CHECK (status IN ('PLANNED', 'ONGOING', 'COMPLETED', 'ARCHIVED')),
    CONSTRAINT ck_trips_dates CHECK (ends_on >= starts_on)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_trips_owner ON trips (owner_id);

CREATE TABLE trip_members
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    trip_id    BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    role       VARCHAR(20) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',        -- REMOVED members keep history, lose write access
    joined_at  DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_members_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    CONSTRAINT fk_members_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_members_trip_user UNIQUE (trip_id, user_id),
    CONSTRAINT ck_members_role CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
    CONSTRAINT ck_members_status CHECK (status IN ('ACTIVE', 'REMOVED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_members_user ON trip_members (user_id);

CREATE TABLE invitations
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    trip_id     BIGINT       NOT NULL,
    email       VARCHAR(255) NULL,                           -- NULL for shareable link invites
    token_hash  VARCHAR(255) NOT NULL,                       -- only the hash of the invite token is stored
    role        VARCHAR(20)  NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    invited_by  BIGINT       NULL,
    expires_at  DATETIME(6)  NOT NULL,
    accepted_at DATETIME(6)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invitations_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitations_inviter FOREIGN KEY (invited_by) REFERENCES users (id),
    CONSTRAINT uq_invitations_token UNIQUE (token_hash),
    CONSTRAINT ck_invitations_role CHECK (role IN ('EDITOR', 'VIEWER')),
    CONSTRAINT ck_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_invitations_trip ON invitations (trip_id);
CREATE INDEX idx_invitations_email ON invitations (email);
