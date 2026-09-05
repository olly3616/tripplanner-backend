package com.voyage.expense.service;

import com.voyage.expense.domain.Expense;
import com.voyage.expense.domain.ExpenseSplit;
import com.voyage.expense.domain.SplitMethod;
import com.voyage.expense.dto.ExpenseParticipant;
import com.voyage.expense.dto.ExpenseRequest;
import com.voyage.expense.dto.ExpenseResponse;
import com.voyage.expense.exchange.ExchangeRateProvider;
import com.voyage.expense.repository.ExpenseRepository;
import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.trip.domain.MemberStatus;
import com.voyage.trip.domain.Trip;
import com.voyage.trip.domain.TripMember;
import com.voyage.trip.domain.TripRole;
import com.voyage.trip.repository.TripMemberRepository;
import com.voyage.trip.repository.TripRepository;
import com.voyage.activity.event.TripActivityEvent;
import com.voyage.trip.service.TripAccessGuard;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripAccessGuard tripAccessGuard;
    private final ExchangeRateProvider exchangeRateProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(Long userId, Long tripId) {
        tripAccessGuard.requireActiveMember(tripId, userId);
        return expenseRepository.findByTripIdWithSplits(tripId).stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional
    public ExpenseResponse create(Long userId, Long tripId, ExpenseRequest request) {
        tripAccessGuard.requireAnyRole(tripId, userId, TripRole.OWNER, TripRole.EDITOR);
        Prepared prepared = prepare(tripId, request);
        Expense expense = expenseRepository.save(Expense.create(
                tripId, request.payerId(), request.title(), request.amountMinor(), request.currency(),
                prepared.rate(), prepared.baseAmountMinor(), request.category(), request.splitMethod(),
                request.spentOn(), request.receiptUrl(), request.note(), prepared.splits()));
        eventPublisher.publishEvent(new TripActivityEvent(tripId, userId,
                TripActivityEvent.EXPENSE_CREATED, "EXPENSE", expense.getId(), expense.getTitle()));
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public ExpenseResponse update(Long userId, Long expenseId, ExpenseRequest request) {
        Expense expense = expenseRepository.findByIdWithSplits(expenseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        tripAccessGuard.requireAnyRole(expense.getTripId(), userId, TripRole.OWNER, TripRole.EDITOR);
        Prepared prepared = prepare(expense.getTripId(), request);
        expense.update(request.payerId(), request.title(), request.amountMinor(), request.currency(),
                prepared.rate(), prepared.baseAmountMinor(), request.category(), request.splitMethod(),
                request.spentOn(), request.receiptUrl(), request.note(), prepared.splits());
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void delete(Long userId, Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        tripAccessGuard.requireAnyRole(expense.getTripId(), userId, TripRole.OWNER, TripRole.EDITOR);
        expenseRepository.delete(expense);
    }

    private record Prepared(BigDecimal rate, long baseAmountMinor, List<ExpenseSplit> splits) {
    }

    private Prepared prepare(Long tripId, ExpenseRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        validateMembership(tripId, request);

        List<ExpenseSplit> splits = computeSplits(request);
        BigDecimal rate = exchangeRateProvider.getRate(request.currency(), trip.getBaseCurrency());
        long baseAmountMinor = BigDecimal.valueOf(request.amountMinor())
                .multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        return new Prepared(rate, baseAmountMinor, splits);
    }

    private void validateMembership(Long tripId, ExpenseRequest request) {
        Set<Long> activeMemberIds = tripMemberRepository.findByTripIdAndStatus(tripId, MemberStatus.ACTIVE)
                .stream().map(TripMember::getUserId).collect(Collectors.toSet());
        if (!activeMemberIds.contains(request.payerId())) {
            throw new BusinessException(ErrorCode.INVALID_EXPENSE, "결제자가 여행 멤버가 아닙니다.");
        }
        List<Long> participantIds = request.participants().stream()
                .map(ExpenseParticipant::userId).toList();
        if (participantIds.size() != Set.copyOf(participantIds).size()) {
            throw new BusinessException(ErrorCode.INVALID_EXPENSE, "참여자가 중복되었습니다.");
        }
        if (!activeMemberIds.containsAll(participantIds)) {
            throw new BusinessException(ErrorCode.INVALID_EXPENSE, "참여자 중 여행 멤버가 아닌 사용자가 있습니다.");
        }
    }

    private List<ExpenseSplit> computeSplits(ExpenseRequest request) {
        List<ExpenseParticipant> participants = request.participants();
        long amount = request.amountMinor();
        return switch (request.splitMethod()) {
            case EQUAL -> allocateByWeights(amount, participants, p -> 1L);
            case RATIO -> allocateByWeights(amount, participants, p -> requirePositiveWeight(p));
            case EXACT -> exactSplits(amount, participants);
        };
    }

    private List<ExpenseSplit> allocateByWeights(long amount, List<ExpenseParticipant> participants,
                                                 java.util.function.ToLongFunction<ExpenseParticipant> weightFn) {
        long[] weights = participants.stream().mapToLong(weightFn).toArray();
        long[] allocation = ProportionalAllocator.allocate(amount, weights);
        return buildSplits(participants, allocation);
    }

    private List<ExpenseSplit> exactSplits(long amount, List<ExpenseParticipant> participants) {
        long[] allocation = new long[participants.size()];
        long sum = 0;
        for (int i = 0; i < participants.size(); i++) {
            Long value = participants.get(i).amountMinor();
            if (value == null || value < 0) {
                throw new BusinessException(ErrorCode.INVALID_EXPENSE, "직접 분할 금액이 올바르지 않습니다.");
            }
            allocation[i] = value;
            sum += value;
        }
        if (sum != amount) {
            throw new BusinessException(ErrorCode.EXPENSE_SPLIT_MISMATCH);
        }
        return buildSplits(participants, allocation);
    }

    private long requirePositiveWeight(ExpenseParticipant participant) {
        if (participant.weight() == null || participant.weight() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_EXPENSE, "비율은 양수여야 합니다.");
        }
        return participant.weight();
    }

    private List<ExpenseSplit> buildSplits(List<ExpenseParticipant> participants, long[] allocation) {
        return java.util.stream.IntStream.range(0, participants.size())
                .mapToObj(i -> ExpenseSplit.of(participants.get(i).userId(), allocation[i]))
                .toList();
    }
}
