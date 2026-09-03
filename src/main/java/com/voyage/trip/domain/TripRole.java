package com.voyage.trip.domain;

/** Membership role within a single trip. Authorization is always per-trip. */
public enum TripRole {
    OWNER,
    EDITOR,
    VIEWER
}
