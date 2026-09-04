package com.voyage.place;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyage.place.domain.PlaceStatus;
import com.voyage.place.domain.SavedPlace;
import com.voyage.place.dto.PlaceResponse;
import com.voyage.place.dto.SavePlaceRequest;
import com.voyage.place.repository.SavedPlaceRepository;
import com.voyage.place.search.PlaceSearchProvider;
import com.voyage.place.service.PlaceService;
import com.voyage.trip.service.TripAccessGuard;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private SavedPlaceRepository savedPlaceRepository;
    @Mock
    private TripAccessGuard tripAccessGuard;
    @Mock
    private PlaceSearchProvider placeSearchProvider;
    @InjectMocks
    private PlaceService placeService;

    private static SavedPlace place(String name, PlaceStatus status, List<String> tags) {
        return SavedPlace.create(1L, "STUB", null, name, "addr", 33.4, 126.5, "cafe", status, tags, null);
    }

    @Test
    void save_existingProviderPlace_returnsExistingWithoutInsert() {
        SavedPlace existing = place("성산일출봉", PlaceStatus.WISH, List.of());
        when(savedPlaceRepository.findByTripIdAndProviderPlaceId(1L, "pid-1"))
                .thenReturn(Optional.of(existing));

        PlaceResponse response = placeService.save(7L, 1L, new SavePlaceRequest(
                "STUB", "pid-1", "성산일출봉", "addr", 33.4, 126.5, "관광", null, null, null));

        assertEquals("성산일출봉", response.name());
        verify(savedPlaceRepository, never()).save(any());
    }

    @Test
    void save_newProviderPlace_persists() {
        when(savedPlaceRepository.findByTripIdAndProviderPlaceId(1L, "pid-2")).thenReturn(Optional.empty());
        when(savedPlaceRepository.save(any(SavedPlace.class))).thenAnswer(inv -> inv.getArgument(0));

        PlaceResponse response = placeService.save(7L, 1L, new SavePlaceRequest(
                "STUB", "pid-2", "카페", "addr", 33.4, 126.5, "cafe", null, List.of("분위기"), null));

        assertEquals("카페", response.name());
        assertEquals(PlaceStatus.WISH, response.status());
        verify(savedPlaceRepository).save(any(SavedPlace.class));
    }

    @Test
    void list_filtersByStatusAndTag() {
        when(savedPlaceRepository.findByTripIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(
                place("A", PlaceStatus.CONFIRMED, List.of("cafe")),
                place("B", PlaceStatus.WISH, List.of("food")),
                place("C", PlaceStatus.CONFIRMED, List.of("food"))));

        assertEquals(2, placeService.list(7L, 1L, PlaceStatus.CONFIRMED, null, null).size());
        assertEquals(2, placeService.list(7L, 1L, null, null, "food").size());
        assertEquals(1, placeService.list(7L, 1L, PlaceStatus.CONFIRMED, null, "food").size());
    }

    @Test
    void search_delegatesToProvider() {
        when(placeSearchProvider.search("제주")).thenReturn(List.of(
                new com.voyage.place.search.PlaceSearchResult("STUB", "s1", "제주 명소", "addr", 33.4, 126.5, "관광")));

        assertEquals(1, placeService.search(7L, 1L, "제주").size());
    }
}
