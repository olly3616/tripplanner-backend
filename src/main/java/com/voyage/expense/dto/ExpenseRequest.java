package com.voyage.expense.dto;

import com.voyage.expense.domain.SplitMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Create/replace an expense. Splits are computed server-side from the method + participants. */
public record ExpenseRequest(
        @NotBlank @Size(max = 150) String title,
        @NotNull @Positive Long amountMinor,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @Size(max = 50) String category,
        @NotNull Long payerId,
        @NotNull SplitMethod splitMethod,
        @NotNull LocalDate spentOn,
        @Size(max = 512) String receiptUrl,
        @Size(max = 1000) String note,
        @NotEmpty @Valid List<ExpenseParticipant> participants
) {
}
