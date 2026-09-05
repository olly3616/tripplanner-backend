package com.voyage.expense.dto;

import com.voyage.expense.domain.Expense;
import com.voyage.expense.domain.SplitMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ExpenseResponse(
        Long id,
        Long tripId,
        Long payerId,
        String title,
        long amountMinor,
        String currency,
        BigDecimal exchangeRate,
        long baseAmountMinor,
        String category,
        SplitMethod splitMethod,
        LocalDate spentOn,
        String receiptUrl,
        String note,
        List<SplitResponse> splits,
        Instant createdAt,
        Instant updatedAt
) {

    public record SplitResponse(Long userId, long amountMinor) {
    }

    public static ExpenseResponse from(Expense expense) {
        List<SplitResponse> splits = expense.getSplits().stream()
                .map(s -> new SplitResponse(s.getUserId(), s.getAmountMinor()))
                .toList();
        return new ExpenseResponse(
                expense.getId(), expense.getTripId(), expense.getPayerId(), expense.getTitle(),
                expense.getAmountMinor(), expense.getCurrency(), expense.getExchangeRate(),
                expense.getBaseAmountMinor(), expense.getCategory(), expense.getSplitMethod(),
                expense.getSpentOn(), expense.getReceiptUrl(), expense.getNote(), splits,
                expense.getCreatedAt(), expense.getUpdatedAt());
    }
}
