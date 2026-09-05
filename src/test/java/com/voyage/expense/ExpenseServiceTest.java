package com.voyage.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.voyage.expense.domain.Expense;
import com.voyage.expense.domain.SplitMethod;
import com.voyage.expense.dto.ExpenseParticipant;
import com.voyage.expense.dto.ExpenseRequest;
import com.voyage.expense.dto.ExpenseResponse;
import com.voyage.expense.exchange.StubExchangeRateProvider;
import com.voyage.expense.repository.ExpenseRepository;
import com.voyage.expense.service.ExpenseService;
import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.trip.domain.Trip;
import com.voyage.trip.domain.TripMember;
import com.voyage.trip.repository.TripMemberRepository;
import com.voyage.trip.repository.TripRepository;
import com.voyage.trip.service.TripAccessGuard;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripMemberRepository tripMemberRepository;
    @Mock
    private TripAccessGuard tripAccessGuard;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(expenseRepository, tripRepository, tripMemberRepository,
                tripAccessGuard, new StubExchangeRateProvider(), eventPublisher);
    }

    private void stubTripAndMembers(String baseCurrency, Long... memberIds) {
        Trip trip = Trip.create(1L, "제주 여행", "제주",
                LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 17), baseCurrency, "Asia/Seoul", null);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        List<TripMember> members = java.util.Arrays.stream(memberIds)
                .map(id -> TripMember.member(1L, id, com.voyage.trip.domain.TripRole.EDITOR))
                .toList();
        when(tripMemberRepository.findByTripIdAndStatus(any(), any())).thenReturn(members);
    }

    private void stubSave() {
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ExpenseParticipant equalParticipant(Long userId) {
        return new ExpenseParticipant(userId, null, null);
    }

    @Test
    void create_equalSplit_sameCurrency() {
        stubTripAndMembers("KRW", 1L, 2L, 3L);
        stubSave();
        ExpenseRequest request = new ExpenseRequest("숙소비", 300L, "KRW", "숙박", 1L,
                SplitMethod.EQUAL, LocalDate.of(2026, 8, 14), null, null,
                List.of(equalParticipant(1L), equalParticipant(2L), equalParticipant(3L)));

        ExpenseResponse response = expenseService.create(9L, 1L, request);

        assertEquals(300L, response.baseAmountMinor());
        assertEquals(300L, response.splits().stream().mapToLong(ExpenseResponse.SplitResponse::amountMinor).sum());
        assertEquals(3, response.splits().size());
    }

    @Test
    void create_multiCurrency_snapshotsBaseAmount() {
        stubTripAndMembers("KRW", 1L, 2L);
        stubSave();
        ExpenseRequest request = new ExpenseRequest("라멘", 10_000L, "JPY", "식비", 1L,
                SplitMethod.EQUAL, LocalDate.of(2026, 8, 14), null, null,
                List.of(equalParticipant(1L), equalParticipant(2L)));

        ExpenseResponse response = expenseService.create(9L, 1L, request);

        // 10000 JPY minor * 9.5 = 95000 KRW minor
        assertEquals(95_000L, response.baseAmountMinor());
    }

    @Test
    void create_exactSplitMismatch_throws() {
        stubTripAndMembers("KRW", 1L, 2L);
        ExpenseRequest request = new ExpenseRequest("점심", 10_000L, "KRW", "식비", 1L,
                SplitMethod.EXACT, LocalDate.of(2026, 8, 14), null, null,
                List.of(new ExpenseParticipant(1L, null, 4_000L),
                        new ExpenseParticipant(2L, null, 5_000L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> expenseService.create(9L, 1L, request));
        assertEquals(ErrorCode.EXPENSE_SPLIT_MISMATCH, ex.getErrorCode());
    }

    @Test
    void create_participantNotMember_throws() {
        stubTripAndMembers("KRW", 1L, 2L);
        ExpenseRequest request = new ExpenseRequest("택시", 9_000L, "KRW", "교통", 1L,
                SplitMethod.EQUAL, LocalDate.of(2026, 8, 14), null, null,
                List.of(equalParticipant(1L), equalParticipant(99L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> expenseService.create(9L, 1L, request));
        assertEquals(ErrorCode.INVALID_EXPENSE, ex.getErrorCode());
    }
}
