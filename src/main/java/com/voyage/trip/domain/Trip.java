package com.voyage.trip.domain;

import com.voyage.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trips")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 150)
    private String destination;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status;

    @Column(name = "cover_image_url", length = 512)
    private String coverImageUrl;

    @Builder(access = AccessLevel.PRIVATE)
    private Trip(Long ownerId, String title, String destination, LocalDate startsOn, LocalDate endsOn,
                String baseCurrency, String timezone, TripStatus status, String coverImageUrl) {
        this.ownerId = ownerId;
        this.title = title;
        this.destination = destination;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
        this.baseCurrency = baseCurrency;
        this.timezone = timezone;
        this.status = status;
        this.coverImageUrl = coverImageUrl;
    }

    public static Trip create(Long ownerId, String title, String destination, LocalDate startsOn,
                              LocalDate endsOn, String baseCurrency, String timezone, String coverImageUrl) {
        return Trip.builder()
                .ownerId(ownerId)
                .title(title)
                .destination(destination)
                .startsOn(startsOn)
                .endsOn(endsOn)
                .baseCurrency(baseCurrency)
                .timezone(timezone)
                .status(TripStatus.PLANNED)
                .coverImageUrl(coverImageUrl)
                .build();
    }

    /** Overwrites the editable details. Callers merge partial updates before calling this. */
    public void updateDetails(String title, String destination, LocalDate startsOn, LocalDate endsOn,
                              String baseCurrency, String timezone, String coverImageUrl) {
        this.title = title;
        this.destination = destination;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
        this.baseCurrency = baseCurrency;
        this.timezone = timezone;
        this.coverImageUrl = coverImageUrl;
    }

    public void changeStatus(TripStatus status) {
        this.status = status;
    }
}
