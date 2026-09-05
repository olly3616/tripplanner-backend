package com.voyage.ai.service;

import com.voyage.ai.dto.AiDraftRequest;
import com.voyage.ai.dto.ItineraryDraftResponse;
import com.voyage.ai.provider.AiItineraryProvider;
import com.voyage.ai.provider.DraftContext;
import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.place.repository.SavedPlaceRepository;
import com.voyage.trip.domain.Trip;
import com.voyage.trip.repository.TripRepository;
import com.voyage.trip.service.TripAccessGuard;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiDraftService {

    private static final int DEFAULT_ITEMS_PER_DAY = 3;

    private final TripAccessGuard tripAccessGuard;
    private final TripRepository tripRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final AiItineraryProvider aiItineraryProvider;

    @Transactional(readOnly = true)
    public ItineraryDraftResponse suggest(Long userId, Long tripId, AiDraftRequest request) {
        tripAccessGuard.requireActiveMember(tripId, userId);
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        List<DraftContext.PlaceCandidate> candidates =
                savedPlaceRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
                        .map(p -> new DraftContext.PlaceCandidate(
                                p.getId(), p.getName(), p.getCategory(), p.getStatus().name()))
                        .toList();

        int itemsPerDay = request != null && request.itemsPerDay() != null
                ? request.itemsPerDay() : DEFAULT_ITEMS_PER_DAY;
        List<String> preferred = request != null ? request.preferredCategories() : null;

        return aiItineraryProvider.suggest(new DraftContext(
                trip.getStartsOn(), trip.getEndsOn(), candidates, itemsPerDay, preferred));
    }
}
