-- V6: polls, their options, and per-user votes.

CREATE TABLE polls
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    trip_id         BIGINT       NOT NULL,
    created_by      BIGINT       NOT NULL,
    title           VARCHAR(200) NOT NULL,
    multiple_choice BIT          NOT NULL DEFAULT 0,
    anonymous       BIT          NOT NULL DEFAULT 0,
    closes_at       DATETIME(6)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_polls_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    CONSTRAINT fk_polls_creator FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_polls_trip ON polls (trip_id);

CREATE TABLE poll_options
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    poll_id    BIGINT       NOT NULL,
    label      VARCHAR(200) NOT NULL,
    sort_order INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_options_poll FOREIGN KEY (poll_id) REFERENCES polls (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_options_poll ON poll_options (poll_id);

CREATE TABLE votes
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    poll_id    BIGINT      NOT NULL,
    option_id  BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_votes_poll FOREIGN KEY (poll_id) REFERENCES polls (id) ON DELETE CASCADE,
    CONSTRAINT fk_votes_option FOREIGN KEY (option_id) REFERENCES poll_options (id) ON DELETE CASCADE,
    CONSTRAINT fk_votes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_votes_option_user UNIQUE (poll_id, option_id, user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_votes_poll ON votes (poll_id);
