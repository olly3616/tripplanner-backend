package com.voyage.expense.dto;

import java.util.List;

/** Net balances and recommended transfers for a trip, in base-currency minor units. */
public record SettlementResponse(
        String baseCurrency,
        long totalBaseMinor,
        List<Balance> balances,
        List<TransferView> transfers,
        List<CategoryTotal> categoryTotals
) {

    /** userId's net: positive = should receive, negative = owes. */
    public record Balance(Long userId, long netMinor) {
    }

    public record TransferView(Long fromUserId, Long toUserId, long amountMinor) {
    }

    public record CategoryTotal(String category, long totalBaseMinor) {
    }
}
