package com.voyage.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.trip.domain.MemberStatus;
import com.voyage.trip.domain.Trip;
import com.voyage.trip.domain.TripMember;
import com.voyage.trip.domain.TripRole;
import com.voyage.trip.domain.TripStatus;
import com.voyage.trip.dto.CreateTripRequest;
import com.voyage.trip.dto.TripResponse;
import com.voyage.trip.repository.TripMemberRepository;
import com.voyage.trip.repository.TripRepository;
import com.voyage.trip.service.TripAccessGuard;
import com.voyage.trip.service.TripService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripMemberRepository tripMemberRepository;
    @Mock
    private TripAccessGuard tripAccessGuard;
    @InjectMocks
    private TripService tripService;

    private static CreateTripRequest jejuRequest(LocalDate start, LocalDate end) {
        return new CreateTripRequest("제주 여름 여행", "제주", start, end, "KRW", "Asia/Seoul", null);
    }

    @Test
    void create_registersCreatorAsOwnerAndReturnsPlanned() {
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        TripResponse response = tripService.create(
                7L, jejuRequest(LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 17)));

        assertEquals("제주 여름 여행", response.title());
        assertEquals(TripStatus.PLANNED, response.status());
        assertEquals(TripRole.OWNER, response.myRole());
        assertEquals(1, response.memberCount());

        ArgumentCaptor<TripMember> member = ArgumentCaptor.forClass(TripMember.class);
        verify(tripMemberRepository).save(member.capture());
        assertEquals(TripRole.OWNER, member.getValue().getRole());
        assertEquals(7L, member.getValue().getUserId());
    }

    @Test
    void create_endDateBeforeStartDate_throwsAndSavesNothing() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                tripService.create(7L, jejuRequest(LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 14))));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        verify(tripRepository, never()).save(any());
        verify(tripMemberRepository, never()).save(any());
    }

    @Test
    void changeStatus_ownerChangesToCompleted() {
        Trip trip = Trip.create(7L, "제주 여름 여행", "제주",
                LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 17), "KRW", "Asia/Seoul", null);
        when(tripAccessGuard.requireOwner(1L, 7L)).thenReturn(TripMember.owner(1L, 7L));
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripMemberRepository.countByTripIdAndStatus(1L, MemberStatus.ACTIVE)).thenReturn(3L);

        TripResponse response = tripService.changeStatus(7L, 1L, TripStatus.COMPLETED);

        assertEquals(TripStatus.COMPLETED, response.status());
        assertEquals(TripStatus.COMPLETED, trip.getStatus());
        assertEquals(3, response.memberCount());
    }

    @Test
    void delete_authorizesOwnerThenDeletes() {
        when(tripAccessGuard.requireOwner(1L, 7L)).thenReturn(TripMember.owner(1L, 7L));

        tripService.delete(7L, 1L);

        verify(tripAccessGuard).requireOwner(1L, 7L);
        verify(tripRepository).deleteById(1L);
    }

    @Test
    void get_nonMember_propagatesNotFound() {
        when(tripAccessGuard.requireActiveMember(1L, 99L))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

        assertThrows(BusinessException.class, () -> tripService.get(99L, 1L));
        verify(tripRepository, never()).findById(eq(1L));
    }
}
