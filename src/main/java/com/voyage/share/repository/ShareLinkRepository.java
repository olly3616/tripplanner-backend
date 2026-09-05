package com.voyage.share.repository;

import com.voyage.share.domain.ShareLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    Optional<ShareLink> findByTokenHash(String tokenHash);

    Optional<ShareLink> findByIdAndTripId(Long id, Long tripId);

    List<ShareLink> findByTripIdOrderByCreatedAtDesc(Long tripId);
}
