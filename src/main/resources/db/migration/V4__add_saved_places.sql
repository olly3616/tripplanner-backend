-- V4: saved places per trip, and link itinerary items to them.
-- Dedup: a provider place can be saved once per trip (uq trip_id + provider_place_id).
-- Manual places have NULL provider_place_id (MySQL allows multiple NULLs in a unique index).

CREATE TABLE saved_places
(
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    trip_id           BIGINT        NOT NULL,
    provider          VARCHAR(30)   NULL,
    provider_place_id VARCHAR(255)  NULL,
    name              VARCHAR(200)  NOT NULL,
    address           VARCHAR(500)  NULL,
    latitude          DECIMAL(10, 7) NULL,
    longitude         DECIMAL(10, 7) NULL,
    category          VARCHAR(100)  NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'WISH',
    tags              VARCHAR(500)  NULL,
    note              VARCHAR(1000) NULL,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_places_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    CONSTRAINT uq_places_provider UNIQUE (trip_id, provider_place_id),
    CONSTRAINT ck_places_status CHECK (status IN ('WISH', 'CONSIDERING', 'CONFIRMED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_places_trip ON saved_places (trip_id);

-- Link itinerary items to a saved place; clearing the place keeps the item.
ALTER TABLE itinerary_items
    ADD CONSTRAINT fk_itinerary_place FOREIGN KEY (place_id) REFERENCES saved_places (id) ON DELETE SET NULL;
