package com.voyage.poll.repository;

import com.voyage.poll.domain.Poll;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollRepository extends JpaRepository<Poll, Long> {

    List<Poll> findByTripIdOrderByCreatedAtDesc(Long tripId);

    @Query("select distinct p from Poll p left join fetch p.options where p.id = :id")
    Optional<Poll> findByIdWithOptions(@Param("id") Long id);
}
