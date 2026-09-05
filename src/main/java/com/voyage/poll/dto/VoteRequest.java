package com.voyage.poll.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** The full set of options this user is voting for (replaces any previous vote). */
public record VoteRequest(
        @NotEmpty List<Long> optionIds
) {
}
