package com.voyage.trip.repository;

/** Projection for grouped active-member counts. */
public interface TripMemberCount {

    Long getTripId();

    long getCount();
}
