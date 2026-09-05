package com.voyage.expense.service;

import com.voyage.expense.domain.Expense;
import com.voyage.expense.domain.ExpenseSplit;
import com.voyage.expense.dto.SettlementResponse;
import com.voyage.expense.repository.ExpenseRepository;
import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.trip.domain.Trip;
import com.voyage.trip.repository.TripRepository;
import com.voyage.trip.service.TripAccessGuard;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final String UNCATEGORIZED = "기타";

    private final ExpenseRepository expenseRepository;
    private final TripRepository tripRepository;
    private final TripAccessGuard tripAccessGuard;

    @Transactional(readOnly = true)
    public SettlementResponse settle(Long userId, Long tripId) {
        tripAccessGuard.requireActiveMember(tripId, userId);
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        Map<Long, Long> net = new LinkedHashMap<>();
        Map<String, Long> categoryTotals = new TreeMap<>();
        long total = 0;

        for (Expense expense : expenseRepository.findByTripIdWithSplits(tripId)) {
            long base = expense.getBaseAmountMinor();
            total += base;
            net.merge(expense.getPayerId(), base, Long::sum);

            List<ExpenseSplit> splits = expense.getSplits();
            long[] weights = splits.stream().mapToLong(ExpenseSplit::getAmountMinor).toArray();
            long[] baseShares = ProportionalAllocator.allocate(base, weights);
            for (int i = 0; i < splits.size(); i++) {
                net.merge(splits.get(i).getUserId(), -baseShares[i], Long::sum);
            }

            String category = expense.getCategory() != null ? expense.getCategory() : UNCATEGORIZED;
            categoryTotals.merge(category, base, Long::sum);
        }

        List<SettlementResponse.Balance> balances = net.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new SettlementResponse.Balance(e.getKey(), e.getValue()))
                .toList();

        List<SettlementResponse.TransferView> transfers = new ArrayList<>();
        for (Transfer t : SettlementCalculator.settle(net)) {
            transfers.add(new SettlementResponse.TransferView(t.fromUserId(), t.toUserId(), t.amountMinor()));
        }
        transfers.sort(Comparator.comparingLong(SettlementResponse.TransferView::fromUserId)
                .thenComparingLong(SettlementResponse.TransferView::toUserId));

        List<SettlementResponse.CategoryTotal> categories = categoryTotals.entrySet().stream()
                .map(e -> new SettlementResponse.CategoryTotal(e.getKey(), e.getValue()))
                .toList();

        return new SettlementResponse(trip.getBaseCurrency(), total, balances, transfers, categories);
    }
}
