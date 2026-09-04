package com.voyage.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.trip.domain.Invitation;
import com.voyage.trip.domain.MemberStatus;
import com.voyage.trip.domain.TripMember;
import com.voyage.trip.domain.TripRole;
import com.voyage.trip.dto.AcceptInvitationResponse;
import com.voyage.trip.dto.ChangeRoleRequest;
import com.voyage.trip.dto.InviteRequest;
import com.voyage.trip.dto.InviteResponse;
import com.voyage.trip.repository.InvitationRepository;
import com.voyage.trip.repository.TripMemberRepository;
import com.voyage.trip.service.TripAccessGuard;
import com.voyage.trip.service.TripMemberService;
import com.voyage.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripMemberServiceTest {

    @Mock
    private TripAccessGuard tripAccessGuard;
    @Mock
    private TripMemberRepository tripMemberRepository;
    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private TripMemberService tripMemberService;

    @Test
    void invite_generatesTokenAndPersistsInvitation() {
        when(tripAccessGuard.requireOwner(1L, 7L)).thenReturn(TripMember.owner(1L, 7L));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        InviteResponse response = tripMemberService.invite(7L, 1L,
                new InviteRequest("guest@voyage.com", TripRole.EDITOR));

        assertNotNull(response.token());
        assertEquals(TripRole.EDITOR, response.role());
        verify(invitationRepository).save(any(Invitation.class));
    }

    @Test
    void invite_ownerRole_rejected() {
        when(tripAccessGuard.requireOwner(1L, 7L)).thenReturn(TripMember.owner(1L, 7L));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                tripMemberService.invite(7L, 1L, new InviteRequest(null, TripRole.OWNER)));

        assertEquals(ErrorCode.INVALID_ROLE, ex.getErrorCode());
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void accept_newMember_isPersisted() {
        Invitation invitation = Invitation.create(
                1L, "guest@voyage.com", "hash", TripRole.EDITOR, 7L,
                Instant.now().plus(1, ChronoUnit.DAYS));
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));
        when(tripMemberRepository.findByTripIdAndUserId(1L, 42L)).thenReturn(Optional.empty());

        AcceptInvitationResponse response = tripMemberService.accept(42L, "raw-token");

        assertEquals(1L, response.tripId());
        assertEquals(TripRole.EDITOR, response.role());
        verify(tripMemberRepository).save(any(TripMember.class));
    }

    @Test
    void accept_reactivatesRemovedMember() {
        Invitation invitation = Invitation.create(
                1L, null, "hash", TripRole.VIEWER, 7L, Instant.now().plus(1, ChronoUnit.DAYS));
        TripMember removed = TripMember.member(1L, 42L, TripRole.EDITOR);
        removed.remove();
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));
        when(tripMemberRepository.findByTripIdAndUserId(1L, 42L)).thenReturn(Optional.of(removed));

        tripMemberService.accept(42L, "raw-token");

        assertEquals(MemberStatus.ACTIVE, removed.getStatus());
        assertEquals(TripRole.VIEWER, removed.getRole());
        verify(tripMemberRepository, never()).save(any());
    }

    @Test
    void accept_expiredInvitation_rejected() {
        Invitation expired = Invitation.create(
                1L, null, "hash", TripRole.VIEWER, 7L, Instant.now().minus(1, ChronoUnit.DAYS));
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripMemberService.accept(42L, "raw-token"));

        assertEquals(ErrorCode.INVITATION_INVALID, ex.getErrorCode());
    }

    @Test
    void removeMember_owner_cannotBeRemoved() {
        when(tripAccessGuard.requireOwner(1L, 7L)).thenReturn(TripMember.owner(1L, 7L));
        when(tripMemberRepository.findByTripIdAndUserIdAndStatus(1L, 7L, MemberStatus.ACTIVE))
                .thenReturn(Optional.of(TripMember.owner(1L, 7L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripMemberService.removeMember(7L, 1L, 7L));

        assertEquals(ErrorCode.CANNOT_MODIFY_OWNER, ex.getErrorCode());
    }

    @Test
    void changeRole_setsNewRole() {
        TripMember target = TripMember.member(1L, 42L, TripRole.EDITOR);
        when(tripAccessGuard.requireOwner(1L, 7L)).thenReturn(TripMember.owner(1L, 7L));
        when(tripMemberRepository.findByTripIdAndUserIdAndStatus(1L, 42L, MemberStatus.ACTIVE))
                .thenReturn(Optional.of(target));
        when(userRepository.findById(42L)).thenReturn(Optional.of(
                com.voyage.user.domain.User.createEmailUser("guest@voyage.com", "h", "Guest", null, null)));

        tripMemberService.changeRole(7L, 1L, 42L, new ChangeRoleRequest(TripRole.VIEWER));

        assertEquals(TripRole.VIEWER, target.getRole());
    }
}
