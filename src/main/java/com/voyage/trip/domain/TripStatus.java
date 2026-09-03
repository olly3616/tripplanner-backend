package com.voyage.trip.domain;

/**
 * User-controlled trip lifecycle. Not derived from dates: a trip whose dates
 * have passed may still be PLANNED (postponed) and vice versa.
 */
public enum TripStatus {
    PLANNED,
    ONGOING,
    COMPLETED,
    ARCHIVED
}
