package com.voyage.poll.repository;

import com.voyage.poll.domain.Vote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findByPollId(Long pollId);

    List<Vote> findByPollIdAndUserId(Long pollId, Long userId);

    void deleteByPollIdAndUserId(Long pollId, Long userId);

    void deleteByPollId(Long pollId);
}
