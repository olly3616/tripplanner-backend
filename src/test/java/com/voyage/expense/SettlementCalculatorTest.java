package com.voyage.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voyage.expense.service.SettlementCalculator;
import com.voyage.expense.service.Transfer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettlementCalculatorTest {

    @Test
    void twoPeople_singleTransfer() {
        List<Transfer> transfers = SettlementCalculator.settle(Map.of(1L, 50L, 2L, -50L));

        assertEquals(1, transfers.size());
        Transfer t = transfers.get(0);
        assertEquals(2L, t.fromUserId());
        assertEquals(1L, t.toUserId());
        assertEquals(50L, t.amountMinor());
    }

    @Test
    void threePeople_balancesZeroOutAfterTransfers() {
        Map<Long, Long> net = new HashMap<>(Map.of(1L, 130_000L, 2L, -50_000L, 3L, -80_000L));

        List<Transfer> transfers = SettlementCalculator.settle(net);

        // Apply transfers back onto balances; everyone must end at zero.
        Map<Long, Long> applied = new HashMap<>(net);
        for (Transfer t : transfers) {
            applied.merge(t.fromUserId(), t.amountMinor(), Long::sum);
            applied.merge(t.toUserId(), -t.amountMinor(), Long::sum);
        }
        assertTrue(applied.values().stream().allMatch(v -> v == 0));
        // At most n-1 transfers.
        assertTrue(transfers.size() <= 2);
    }

    @Test
    void allSettled_noTransfers() {
        assertTrue(SettlementCalculator.settle(Map.of(1L, 0L, 2L, 0L)).isEmpty());
    }

    @Test
    void threeWayMixed_balancesSumToZeroInput_andZeroAfter() {
        Map<Long, Long> net = new HashMap<>();
        net.put(1L, 60L);
        net.put(2L, -40L);
        net.put(3L, -20L);

        List<Transfer> transfers = SettlementCalculator.settle(net);
        long totalMoved = transfers.stream().mapToLong(Transfer::amountMinor).sum();
        assertEquals(60L, totalMoved);
    }
}
