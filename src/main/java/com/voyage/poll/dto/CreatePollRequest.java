package com.voyage.poll.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record CreatePollRequest(
        @NotBlank @Size(max = 200) String title,
        boolean multipleChoice,
        boolean anonymous,
        @NotNull Instant closesAt,
        @NotEmpty List<@NotBlank @Size(max = 200) String> options
) {
}
