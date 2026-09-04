package com.voyage.itinerary.repository;

import com.voyage.itinerary.domain.ItineraryItem;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {

    List<ItineraryItem> findByTripIdOrderByDateAscSortOrderAsc(Long tripId);

    @Query("""
            select coalesce(max(i.sortOrder), -1)
            from ItineraryItem i
            where i.tripId = :tripId and i.date = :date
            """)
    int findMaxSortOrder(@Param("tripId") Long tripId, @Param("date") LocalDate date);
}
