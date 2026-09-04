package com.voyage.itinerary.domain;

import com.voyage.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "itinerary_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItineraryItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    /** Optional link to a saved place (wired up in the places milestone). */
    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "item_date", nullable = false)
    private LocalDate date;

    @Column(name = "starts_at")
    private LocalTime startsAt;

    @Column(name = "ends_at")
    private LocalTime endsAt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(length = 30)
    private String transport;

    @Column(length = 1000)
    private String note;

    @Version
    private Long version;

    @Builder(access = AccessLevel.PRIVATE)
    private ItineraryItem(Long tripId, Long placeId, LocalDate date, LocalTime startsAt, LocalTime endsAt,
                          int sortOrder, String transport, String note) {
        this.tripId = tripId;
        this.placeId = placeId;
        this.date = date;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.sortOrder = sortOrder;
        this.transport = transport;
        this.note = note;
    }

    public static ItineraryItem create(Long tripId, Long placeId, LocalDate date, LocalTime startsAt,
                                        LocalTime endsAt, int sortOrder, String transport, String note) {
        return ItineraryItem.builder()
                .tripId(tripId)
                .placeId(placeId)
                .date(date)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .sortOrder(sortOrder)
                .transport(transport)
                .note(note)
                .build();
    }

    public void updateDetails(Long placeId, LocalDate date, LocalTime startsAt, LocalTime endsAt,
                              String transport, String note) {
        this.placeId = placeId;
        this.date = date;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.transport = transport;
        this.note = note;
    }

    /** Used by the reorder operation to move an item to a new date/position. */
    public void moveTo(LocalDate date, int sortOrder) {
        this.date = date;
        this.sortOrder = sortOrder;
    }
}
