package com.voyage.trip.repository;

import com.voyage.trip.domain.Invitation;
import com.voyage.trip.domain.InvitationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    Optional<Invitation> findByIdAndTripId(Long id, Long tripId);

    List<Invitation> findByTripIdAndStatus(Long tripId, InvitationStatus status);
}
