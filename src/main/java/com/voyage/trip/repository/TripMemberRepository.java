package com.voyage.trip.repository;

import com.voyage.trip.domain.MemberStatus;
import com.voyage.trip.domain.TripMember;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripMemberRepository extends JpaRepository<TripMember, Long> {

    Optional<TripMember> findByTripIdAndUserIdAndStatus(Long tripId, Long userId, MemberStatus status);

    Optional<TripMember> findByTripIdAndUserId(Long tripId, Long userId);

    List<TripMember> findByTripIdAndStatus(Long tripId, MemberStatus status);

    List<TripMember> findByUserIdAndStatus(Long userId, MemberStatus status);

    long countByTripIdAndStatus(Long tripId, MemberStatus status);

    /** Active-member counts for many trips in one query (avoids N+1 in listings). */
    @Query("""
            select m.tripId as tripId, count(m) as count
            from TripMember m
            where m.tripId in :tripIds and m.status = :status
            group by m.tripId
            """)
    List<TripMemberCount> countByTripIds(@Param("tripIds") Collection<Long> tripIds,
                                         @Param("status") MemberStatus status);
}
