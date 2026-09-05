package com.voyage.poll.dto;

import java.time.Instant;
import java.util.List;

public record PollResponse(
        Long id,
        Long tripId,
        Long createdBy,
        String title,
        boolean multipleChoice,
        boolean anonymous,
        Instant closesAt,
        boolean closed,
        List<OptionResult> options,
        int totalVoters,
        List<Long> myOptionIds
) {

    /** voterIds is null for anonymous polls; otherwise lists who picked this option. */
    public record OptionResult(Long id, String label, int voteCount, List<Long> voterIds) {
    }
}
