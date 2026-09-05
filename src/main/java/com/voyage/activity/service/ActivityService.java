package com.voyage.activity.service;

import com.voyage.activity.dto.ActivityResponse;
import com.voyage.activity.repository.ActivityLogRepository;
import com.voyage.trip.service.TripAccessGuard;
import java.util.List;
import org.springframework.data.domain.Limit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private static final int MAX_RESULTS = 100;

    private final ActivityLogRepository activityLogRepository;
    private final TripAccessGuard tripAccessGuard;

    @Transactional(readOnly = true)
    public List<ActivityResponse> list(Long userId, Long tripId) {
        tripAccessGuard.requireActiveMember(tripId, userId);
        return activityLogRepository.findByTripIdOrderByCreatedAtDesc(tripId, Limit.of(MAX_RESULTS)).stream()
                .map(ActivityResponse::from)
                .toList();
    }
}
