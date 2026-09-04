package com.voyage.itinerary.service;

import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.itinerary.domain.ItineraryItem;
import com.voyage.itinerary.dto.CreateItineraryItemRequest;
import com.voyage.itinerary.dto.ItineraryItemResponse;
import com.voyage.itinerary.dto.ReorderRequest;
import com.voyage.itinerary.dto.UpdateItineraryItemRequest;
import com.voyage.itinerary.repository.ItineraryItemRepository;
import com.voyage.trip.domain.TripRole;
import com.voyage.trip.service.TripAccessGuard;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItineraryService {

    private final ItineraryItemRepository itineraryItemRepository;
    private final TripAccessGuard tripAccessGuard;

    @Transactional(readOnly = true)
    public List<ItineraryItemResponse> list(Long userId, Long tripId) {
        tripAccessGuard.requireActiveMember(tripId, userId);
        return itineraryItemRepository.findByTripIdOrderByDateAscSortOrderAsc(tripId).stream()
                .map(ItineraryItemResponse::from)
                .toList();
    }

    @Transactional
    public ItineraryItemResponse create(Long userId, Long tripId, CreateItineraryItemRequest request) {
        requireEditor(tripId, userId);
        int nextSortOrder = itineraryItemRepository.findMaxSortOrder(tripId, request.date()) + 1;
        ItineraryItem item = itineraryItemRepository.save(ItineraryItem.create(
                tripId, request.placeId(), request.date(), request.startsAt(), request.endsAt(),
                nextSortOrder, request.transport(), request.note()));
        return ItineraryItemResponse.from(item);
    }

    @Transactional
    public ItineraryItemResponse update(Long userId, Long itemId, UpdateItineraryItemRequest request) {
        ItineraryItem item = findItem(itemId);
        requireEditor(item.getTripId(), userId);
        if (!item.getVersion().equals(request.version())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        LocalDate date = request.date() != null ? request.date() : item.getDate();
        Long placeId = request.placeId() != null ? request.placeId() : item.getPlaceId();
        LocalTime startsAt = request.startsAt() != null ? request.startsAt() : item.getStartsAt();
        LocalTime endsAt = request.endsAt() != null ? request.endsAt() : item.getEndsAt();
        String transport = request.transport() != null ? request.transport() : item.getTransport();
        String note = request.note() != null ? request.note() : item.getNote();

        item.updateDetails(placeId, date, startsAt, endsAt, transport, note);
        // Flush so the @Version increment is reflected in the returned payload.
        itineraryItemRepository.flush();
        return ItineraryItemResponse.from(item);
    }

    @Transactional
    public void delete(Long userId, Long itemId) {
        ItineraryItem item = findItem(itemId);
        requireEditor(item.getTripId(), userId);
        itineraryItemRepository.delete(item);
    }

    @Transactional
    public List<ItineraryItemResponse> reorder(Long userId, ReorderRequest request) {
        List<Long> ids = request.items().stream().map(ReorderRequest.Entry::itemId).toList();
        List<ItineraryItem> items = itineraryItemRepository.findAllById(ids);
        if (items.size() != ids.size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Long tripId = items.get(0).getTripId();
        if (!items.stream().allMatch(i -> i.getTripId().equals(tripId))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "여러 여행의 항목을 함께 정렬할 수 없습니다.");
        }
        requireEditor(tripId, userId);

        Map<Long, ReorderRequest.Entry> byId = request.items().stream()
                .collect(Collectors.toMap(ReorderRequest.Entry::itemId, Function.identity()));
        items.forEach(item -> {
            ReorderRequest.Entry entry = byId.get(item.getId());
            item.moveTo(entry.date(), entry.sortOrder());
        });
        return itineraryItemRepository.findByTripIdOrderByDateAscSortOrderAsc(tripId).stream()
                .map(ItineraryItemResponse::from)
                .toList();
    }

    private void requireEditor(Long tripId, Long userId) {
        tripAccessGuard.requireAnyRole(tripId, userId, TripRole.OWNER, TripRole.EDITOR);
    }

    private ItineraryItem findItem(Long itemId) {
        return itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
