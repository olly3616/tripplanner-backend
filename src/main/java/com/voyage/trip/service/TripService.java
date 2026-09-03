package com.voyage.trip.service;

import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.trip.domain.MemberStatus;
import com.voyage.trip.domain.Trip;
import com.voyage.trip.domain.TripMember;
import com.voyage.trip.domain.TripRole;
import com.voyage.trip.domain.TripStatus;
import com.voyage.trip.dto.CreateTripRequest;
import com.voyage.trip.dto.TripResponse;
import com.voyage.trip.dto.UpdateTripRequest;
import com.voyage.trip.repository.TripMemberCount;
import com.voyage.trip.repository.TripMemberRepository;
import com.voyage.trip.repository.TripRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripAccessGuard tripAccessGuard;

    @Transactional
    public TripResponse create(Long userId, CreateTripRequest request) {
        validateDateOrder(request.startsOn(), request.endsOn());
        Trip trip = tripRepository.save(Trip.create(
                userId, request.title(), request.destination(), request.startsOn(),
                request.endsOn(), request.baseCurrency(), request.timezone(), request.coverImageUrl()));
        tripMemberRepository.save(TripMember.owner(trip.getId(), userId));
        return TripResponse.from(trip, TripRole.OWNER, 1);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> listMyTrips(Long userId, TripStatus statusFilter) {
        List<TripMember> memberships = tripMemberRepository.findByUserIdAndStatus(userId, MemberStatus.ACTIVE);
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<Long, TripRole> roleByTrip = memberships.stream()
                .collect(Collectors.toMap(TripMember::getTripId, TripMember::getRole));
        Map<Long, Integer> countByTrip = activeMemberCounts(roleByTrip.keySet());

        return tripRepository.findAllById(roleByTrip.keySet()).stream()
                .filter(trip -> statusFilter == null || trip.getStatus() == statusFilter)
                .sorted(Comparator.comparing(Trip::getStartsOn).reversed())
                .map(trip -> TripResponse.from(
                        trip, roleByTrip.get(trip.getId()), countByTrip.getOrDefault(trip.getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public TripResponse get(Long userId, Long tripId) {
        TripMember membership = tripAccessGuard.requireActiveMember(tripId, userId);
        Trip trip = findTrip(tripId);
        int memberCount = (int) tripMemberRepository.countByTripIdAndStatus(tripId, MemberStatus.ACTIVE);
        return TripResponse.from(trip, membership.getRole(), memberCount);
    }

    @Transactional
    public TripResponse update(Long userId, Long tripId, UpdateTripRequest request) {
        tripAccessGuard.requireOwner(tripId, userId);
        Trip trip = findTrip(tripId);

        String title = requiredText(request.title(), trip.getTitle());
        String baseCurrency = requiredText(request.baseCurrency(), trip.getBaseCurrency());
        String timezone = requiredText(request.timezone(), trip.getTimezone());
        String destination = request.destination() != null ? request.destination() : trip.getDestination();
        String coverImageUrl = request.coverImageUrl() != null ? request.coverImageUrl() : trip.getCoverImageUrl();
        LocalDate startsOn = request.startsOn() != null ? request.startsOn() : trip.getStartsOn();
        LocalDate endsOn = request.endsOn() != null ? request.endsOn() : trip.getEndsOn();
        validateDateOrder(startsOn, endsOn);

        trip.updateDetails(title, destination, startsOn, endsOn, baseCurrency, timezone, coverImageUrl);
        int memberCount = (int) tripMemberRepository.countByTripIdAndStatus(tripId, MemberStatus.ACTIVE);
        return TripResponse.from(trip, TripRole.OWNER, memberCount);
    }

    @Transactional
    public TripResponse changeStatus(Long userId, Long tripId, TripStatus status) {
        tripAccessGuard.requireOwner(tripId, userId);
        Trip trip = findTrip(tripId);
        trip.changeStatus(status);
        int memberCount = (int) tripMemberRepository.countByTripIdAndStatus(tripId, MemberStatus.ACTIVE);
        return TripResponse.from(trip, TripRole.OWNER, memberCount);
    }

    @Transactional
    public void delete(Long userId, Long tripId) {
        tripAccessGuard.requireOwner(tripId, userId);
        // Members and invitations are removed via ON DELETE CASCADE foreign keys.
        tripRepository.deleteById(tripId);
    }

    private Trip findTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private Map<Long, Integer> activeMemberCounts(java.util.Collection<Long> tripIds) {
        return tripMemberRepository.countByTripIds(tripIds, MemberStatus.ACTIVE).stream()
                .collect(Collectors.toMap(TripMemberCount::getTripId, c -> (int) c.getCount()));
    }

    private void validateDateOrder(LocalDate startsOn, LocalDate endsOn) {
        if (endsOn.isBefore(startsOn)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "종료일은 시작일 이후여야 합니다.");
        }
    }

    private String requiredText(String incoming, String current) {
        if (incoming == null) {
            return current;
        }
        if (!StringUtils.hasText(incoming)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "필수 항목은 빈 값일 수 없습니다.");
        }
        return incoming;
    }
}
