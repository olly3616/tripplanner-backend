package com.voyage.expense.dto;

import jakarta.validation.constraints.NotNull;

/**
 * A participant in a split.
 * <ul>
 *   <li>EQUAL: only {@code userId} is used</li>
 *   <li>RATIO: {@code weight} is required (positive)</li>
 *   <li>EXACT: {@code amountMinor} is required (their exact share)</li>
 * </ul>
 */
public record ExpenseParticipant(
        @NotNull Long userId,
        Integer weight,
        Long amountMinor
) {
}
