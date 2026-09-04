package com.voyage.itinerary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;

/** Bulk date/position update for drag-and-drop reordering. */
public record ReorderRequest(
        @NotEmpty @Valid List<Entry> items
) {

    public record Entry(
            @NotNull Long itemId,
            @NotNull LocalDate date,
            @NotNull @PositiveOrZero Integer sortOrder
    ) {
    }
}
