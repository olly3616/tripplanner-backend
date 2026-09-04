-- V3: date-based itinerary items.
-- place_id is nullable and unconstrained for now; a FK to saved_places is added
-- in the places milestone. `version` backs JPA optimistic locking.
-- Column is item_date (DATE is a reserved word in MySQL).

CREATE TABLE itinerary_items
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    trip_id    BIGINT       NOT NULL,
    place_id   BIGINT       NULL,
    item_date  DATE         NOT NULL,
    starts_at  TIME         NULL,
    ends_at    TIME         NULL,
    sort_order INT          NOT NULL,
    transport  VARCHAR(30)  NULL,
    note       VARCHAR(1000) NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_itinerary_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_itinerary_trip_date ON itinerary_items (trip_id, item_date, sort_order);
