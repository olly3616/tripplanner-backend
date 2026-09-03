package com.voyage.trip.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Partial update: {@code null} fields are left unchanged. Optional text fields
 * cannot be cleared to null via this endpoint (out of scope for MVP).
 */
public record UpdateTripRequest(
        @Size(max = 150) String title,
        @Size(max = 150) String destination,
        LocalDate startsOn,
        LocalDate endsOn,
        @Size(min = 3, max = 3) String baseCurrency,
        @Size(max = 64) String timezone,
        @Size(max = 512) String coverImageUrl
) {
}
