package com.voyage.trip.service;

import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.global.util.SecureTokens;
import com.voyage.trip.domain.Invitation;
import com.voyage.trip.domain.InvitationStatus;
import com.voyage.trip.domain.MemberStatus;
import com.voyage.trip.domain.TripMember;
import com.voyage.trip.domain.TripRole;
import com.voyage.trip.dto.AcceptInvitationResponse;
import com.voyage.trip.dto.ChangeRoleRequest;
import com.voyage.trip.dto.InvitationResponse;
import com.voyage.trip.dto.InviteRequest;
import com.voyage.trip.dto.InviteResponse;
import com.voyage.trip.dto.MemberResponse;
import com.voyage.trip.repository.InvitationRepository;
import com.voyage.trip.repository.TripMemberRepository;
import com.voyage.user.domain.User;
import com.voyage.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripMemberService {

    private static final Duration INVITATION_TTL = Duration.ofDays(7);

    private final TripAccessGuard tripAccessGuard;
    private final TripMemberRepository tripMemberRepository;
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long requesterId, Long tripId) {
        tripAccessGuard.requireActiveMember(tripId, requesterId);
        List<TripMember> members = tripMemberRepository.findByTripIdAndStatus(tripId, MemberStatus.ACTIVE);
        Map<Long, User> usersById = userRepository.findAllById(
                        members.stream().map(TripMember::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return members.stream()
                .sorted(Comparator.comparing(TripMember::getJoinedAt))
                .map(m -> MemberResponse.of(m, usersById.get(m.getUserId())))
                .toList();
    }

    @Transactional
    public InviteResponse invite(Long ownerId, Long tripId, InviteRequest request) {
        tripAccessGuard.requireOwner(tripId, ownerId);
        if (request.role() == TripRole.OWNER) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }
        String rawToken = SecureTokens.newToken();
        Invitation invitation = invitationRepository.save(Invitation.create(
                tripId, request.email(), SecureTokens.sha256Hex(rawToken), request.role(),
                ownerId, Instant.now().plus(INVITATION_TTL)));
        return InviteResponse.of(invitation, rawToken);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> listPendingInvitations(Long ownerId, Long tripId) {
        tripAccessGuard.requireOwner(tripId, ownerId);
        return invitationRepository.findByTripIdAndStatus(tripId, InvitationStatus.PENDING).stream()
                .map(InvitationResponse::from)
                .toList();
    }

    @Transactional
    public void revokeInvitation(Long ownerId, Long tripId, Long invitationId) {
        tripAccessGuard.requireOwner(tripId, ownerId);
        Invitation invitation = invitationRepository.findByIdAndTripId(invitationId, tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_INVALID));
        invitation.revoke();
    }

    @Transactional
    public AcceptInvitationResponse accept(Long userId, String rawToken) {
        Invitation invitation = invitationRepository.findByTokenHash(SecureTokens.sha256Hex(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_INVALID));
        if (!invitation.isPending(Instant.now())) {
            throw new BusinessException(ErrorCode.INVITATION_INVALID);
        }
        Long tripId = invitation.getTripId();
        TripRole role = invitation.getRole();

        tripMemberRepository.findByTripIdAndUserId(tripId, userId)
                .ifPresentOrElse(
                        existing -> existing.reactivate(role),
                        () -> tripMemberRepository.save(TripMember.member(tripId, userId, role)));
        invitation.accept(Instant.now());
        return new AcceptInvitationResponse(tripId, role);
    }

    @Transactional
    public MemberResponse changeRole(Long ownerId, Long tripId, Long targetUserId, ChangeRoleRequest request) {
        tripAccessGuard.requireOwner(tripId, ownerId);
        if (request.role() == TripRole.OWNER) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }
        TripMember target = requireActiveTarget(tripId, targetUserId);
        if (target.isOwner()) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_OWNER);
        }
        target.changeRole(request.role());
        return MemberResponse.of(target, findUser(targetUserId));
    }

    @Transactional
    public void removeMember(Long ownerId, Long tripId, Long targetUserId) {
        tripAccessGuard.requireOwner(tripId, ownerId);
        TripMember target = requireActiveTarget(tripId, targetUserId);
        if (target.isOwner()) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_OWNER);
        }
        target.remove();
    }

    private TripMember requireActiveTarget(Long tripId, Long targetUserId) {
        return tripMemberRepository.findByTripIdAndUserIdAndStatus(tripId, targetUserId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
