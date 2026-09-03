package com.voyage.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTripRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 150) String destination,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn,
        @NotBlank @Size(min = 3, max = 3) String baseCurrency,
        @NotBlank @Size(max = 64) String timezone,
        @Size(max = 512) String coverImageUrl
) {
}
