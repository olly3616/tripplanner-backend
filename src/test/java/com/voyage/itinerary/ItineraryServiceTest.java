package com.voyage.itinerary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.itinerary.domain.ItineraryItem;
import com.voyage.itinerary.dto.CreateItineraryItemRequest;
import com.voyage.itinerary.dto.ItineraryItemResponse;
import com.voyage.itinerary.dto.ReorderRequest;
import com.voyage.itinerary.dto.UpdateItineraryItemRequest;
import com.voyage.itinerary.repository.ItineraryItemRepository;
import com.voyage.itinerary.service.ItineraryService;
import com.voyage.trip.service.TripAccessGuard;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ItineraryServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 14);

    @Mock
    private ItineraryItemRepository itineraryItemRepository;
    @Mock
    private TripAccessGuard tripAccessGuard;
    @InjectMocks
    private ItineraryService itineraryService;

    private static ItineraryItem itemWithVersion(Long tripId, long version) {
        ItineraryItem item = ItineraryItem.create(tripId, null, DAY, null, null, 0, null, null);
        ReflectionTestUtils.setField(item, "version", version);
        return item;
    }

    @Test
    void create_assignsNextSortOrder() {
        when(itineraryItemRepository.findMaxSortOrder(1L, DAY)).thenReturn(2);
        when(itineraryItemRepository.save(any(ItineraryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ItineraryItemResponse response = itineraryService.create(7L, 1L,
                new CreateItineraryItemRequest(DAY, null, null, null, "렌터카", "메모"));

        assertEquals(3, response.sortOrder());
        assertEquals("렌터카", response.transport());
    }

    @Test
    void update_versionMismatch_throwsConflict() {
        ItineraryItem item = itemWithVersion(1L, 3L);
        when(itineraryItemRepository.findById(10L)).thenReturn(Optional.of(item));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                itineraryService.update(7L, 10L,
                        new UpdateItineraryItemRequest(1L, null, null, null, null, null, "stale")));

        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    void update_matchingVersion_appliesChanges() {
        ItineraryItem item = itemWithVersion(1L, 3L);
        when(itineraryItemRepository.findById(10L)).thenReturn(Optional.of(item));

        ItineraryItemResponse response = itineraryService.update(7L, 10L,
                new UpdateItineraryItemRequest(3L, null, null, null, null, null, "새 메모"));

        assertEquals("새 메모", response.note());
        verify(itineraryItemRepository).flush();
    }

    @Test
    void reorder_itemsFromDifferentTrips_throws() {
        ItineraryItem a = ItineraryItem.create(1L, null, DAY, null, null, 0, null, null);
        ItineraryItem b = ItineraryItem.create(2L, null, DAY, null, null, 0, null, null);
        ReflectionTestUtils.setField(a, "id", 10L);
        ReflectionTestUtils.setField(b, "id", 11L);
        when(itineraryItemRepository.findAllById(any())).thenReturn(List.of(a, b));

        ReorderRequest request = new ReorderRequest(List.of(
                new ReorderRequest.Entry(10L, DAY, 0),
                new ReorderRequest.Entry(11L, DAY, 1)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> itineraryService.reorder(7L, request));
        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    void delete_authorizesThenDeletes() {
        ItineraryItem item = itemWithVersion(1L, 0L);
        when(itineraryItemRepository.findById(10L)).thenReturn(Optional.of(item));

        itineraryService.delete(7L, 10L);

        verify(tripAccessGuard).requireAnyRole(eq(1L), eq(7L), any(), any());
        verify(itineraryItemRepository).delete(item);
    }

    @Test
    void list_nonMember_propagates() {
        when(tripAccessGuard.requireActiveMember(1L, 99L))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

        assertThrows(BusinessException.class, () -> itineraryService.list(99L, 1L));
        verify(itineraryItemRepository, never()).findByTripIdOrderByDateAscSortOrderAsc(any());
    }
}
