package com.voyage.share.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Options for a share link. All fields optional. */
public record CreateShareLinkRequest(
        @Size(min = 4, max = 100) String password,
        Instant expiresAt,
        Boolean includeExpenses
) {

    public boolean includeExpensesOrDefault() {
        return Boolean.TRUE.equals(includeExpenses);
    }
}
