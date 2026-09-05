package com.voyage.expense.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Turns per-member net balances into a minimal-ish list of transfers using a
 * greedy largest-debtor / largest-creditor match. Assumes balances sum to 0;
 * after applying the transfers every balance is 0.
 */
public final class SettlementCalculator {

    private SettlementCalculator() {
    }

    private static final class Holder {
        final Long userId;
        long amount;

        Holder(Long userId, long amount) {
            this.userId = userId;
            this.amount = amount;
        }
    }

    /**
     * @param netByUser userId -> net balance (positive = should receive, negative = owes)
     * @return transfers that zero out every balance
     */
    public static List<Transfer> settle(Map<Long, Long> netByUser) {
        List<Holder> creditors = new ArrayList<>();
        List<Holder> debtors = new ArrayList<>();
        for (Map.Entry<Long, Long> e : netByUser.entrySet()) {
            if (e.getValue() > 0) {
                creditors.add(new Holder(e.getKey(), e.getValue()));
            } else if (e.getValue() < 0) {
                debtors.add(new Holder(e.getKey(), -e.getValue()));
            }
        }
        // Largest first; tie-break by userId for deterministic output.
        creditors.sort(Comparator.<Holder>comparingLong(h -> h.amount).reversed()
                .thenComparingLong(h -> h.userId));
        debtors.sort(Comparator.<Holder>comparingLong(h -> h.amount).reversed()
                .thenComparingLong(h -> h.userId));

        List<Transfer> transfers = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            Holder debtor = debtors.get(i);
            Holder creditor = creditors.get(j);
            long amount = Math.min(debtor.amount, creditor.amount);
            if (amount > 0) {
                transfers.add(new Transfer(debtor.userId, creditor.userId, amount));
                debtor.amount -= amount;
                creditor.amount -= amount;
            }
            if (debtor.amount == 0) {
                i++;
            }
            if (creditor.amount == 0) {
                j++;
            }
        }
        return transfers;
    }
}
