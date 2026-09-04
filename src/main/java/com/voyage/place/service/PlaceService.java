package com.voyage.place.service;

import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.place.domain.PlaceStatus;
import com.voyage.place.domain.SavedPlace;
import com.voyage.place.dto.PlaceResponse;
import com.voyage.place.dto.PlaceSearchResponse;
import com.voyage.place.dto.SavePlaceRequest;
import com.voyage.place.dto.UpdatePlaceRequest;
import com.voyage.place.repository.SavedPlaceRepository;
import com.voyage.place.search.PlaceSearchProvider;
import com.voyage.trip.domain.TripRole;
import com.voyage.trip.service.TripAccessGuard;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final SavedPlaceRepository savedPlaceRepository;
    private final TripAccessGuard tripAccessGuard;
    private final PlaceSearchProvider placeSearchProvider;

    @Transactional(readOnly = true)
    public List<PlaceSearchResponse> search(Long userId, Long tripId, String query) {
        tripAccessGuard.requireActiveMember(tripId, userId);
        return placeSearchProvider.search(query).stream()
                .map(PlaceSearchResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlaceResponse> list(Long userId, Long tripId, PlaceStatus status, String category, String tag) {
        tripAccessGuard.requireActiveMember(tripId, userId);
        return savedPlaceRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> category == null || category.equalsIgnoreCase(p.getCategory()))
                .filter(p -> tag == null || (p.getTags() != null && p.getTags().contains(tag)))
                .map(PlaceResponse::from)
                .toList();
    }

    @Transactional
    public PlaceResponse save(Long userId, Long tripId, SavePlaceRequest request) {
        requireEditor(tripId, userId);
        // Dedup: re-saving the same provider place opens the existing one.
        if (StringUtils.hasText(request.providerPlaceId())) {
            var existing = savedPlaceRepository.findByTripIdAndProviderPlaceId(tripId, request.providerPlaceId());
            if (existing.isPresent()) {
                return PlaceResponse.from(existing.get());
            }
        }
        SavedPlace place = savedPlaceRepository.save(SavedPlace.create(
                tripId, request.provider(), request.providerPlaceId(), request.name(), request.address(),
                request.latitude(), request.longitude(), request.category(), request.status(),
                request.tags(), request.note()));
        return PlaceResponse.from(place);
    }

    @Transactional
    public PlaceResponse update(Long userId, Long tripId, Long placeId, UpdatePlaceRequest request) {
        requireEditor(tripId, userId);
        SavedPlace place = findPlace(tripId, placeId);
        place.update(
                request.name() != null ? request.name() : place.getName(),
                request.address() != null ? request.address() : place.getAddress(),
                request.category() != null ? request.category() : place.getCategory(),
                request.status() != null ? request.status() : place.getStatus(),
                request.tags() != null ? request.tags() : place.getTags(),
                request.note() != null ? request.note() : place.getNote());
        return PlaceResponse.from(place);
    }

    @Transactional
    public void delete(Long userId, Long tripId, Long placeId) {
        requireEditor(tripId, userId);
        SavedPlace place = findPlace(tripId, placeId);
        savedPlaceRepository.delete(place);
    }

    private void requireEditor(Long tripId, Long userId) {
        tripAccessGuard.requireAnyRole(tripId, userId, TripRole.OWNER, TripRole.EDITOR);
    }

    private SavedPlace findPlace(Long tripId, Long placeId) {
        return savedPlaceRepository.findByIdAndTripId(placeId, tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
