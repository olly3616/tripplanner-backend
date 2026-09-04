package com.voyage.place.repository;

import com.voyage.place.domain.SavedPlace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, Long> {

    List<SavedPlace> findByTripIdOrderByCreatedAtDesc(Long tripId);

    Optional<SavedPlace> findByTripIdAndProviderPlaceId(Long tripId, String providerPlaceId);

    Optional<SavedPlace> findByIdAndTripId(Long id, Long tripId);
}
