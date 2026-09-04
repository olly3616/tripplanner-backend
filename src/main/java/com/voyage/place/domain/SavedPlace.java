package com.voyage.place.domain;

import com.voyage.global.common.BaseTimeEntity;
import com.voyage.global.util.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "saved_places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedPlace extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(length = 30)
    private String provider;

    @Column(name = "provider_place_id")
    private String providerPlaceId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String address;

    private Double latitude;

    private Double longitude;

    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceStatus status;

    @Convert(converter = StringListConverter.class)
    @Column(length = 500)
    private List<String> tags;

    @Column(length = 1000)
    private String note;

    @Builder(access = AccessLevel.PRIVATE)
    private SavedPlace(Long tripId, String provider, String providerPlaceId, String name, String address,
                       Double latitude, Double longitude, String category, PlaceStatus status,
                       List<String> tags, String note) {
        this.tripId = tripId;
        this.provider = provider;
        this.providerPlaceId = providerPlaceId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
        this.status = status;
        this.tags = tags;
        this.note = note;
    }

    public static SavedPlace create(Long tripId, String provider, String providerPlaceId, String name,
                                    String address, Double latitude, Double longitude, String category,
                                    PlaceStatus status, List<String> tags, String note) {
        return SavedPlace.builder()
                .tripId(tripId)
                .provider(provider)
                .providerPlaceId(providerPlaceId)
                .name(name)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .category(category)
                .status(status != null ? status : PlaceStatus.WISH)
                .tags(tags)
                .note(note)
                .build();
    }

    public void update(String name, String address, String category, PlaceStatus status,
                       List<String> tags, String note) {
        this.name = name;
        this.address = address;
        this.category = category;
        this.status = status;
        this.tags = tags;
        this.note = note;
    }
}
