package com.voyage.trip.web;

import com.voyage.auth.security.UserPrincipal;
import com.voyage.trip.dto.ChangeRoleRequest;
import com.voyage.trip.dto.InvitationResponse;
import com.voyage.trip.dto.InviteRequest;
import com.voyage.trip.dto.InviteResponse;
import com.voyage.trip.dto.MemberResponse;
import com.voyage.trip.service.TripMemberService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}")
@RequiredArgsConstructor
public class TripMemberController {

    private final TripMemberService tripMemberService;

    @GetMapping("/members")
    public List<MemberResponse> listMembers(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable Long tripId) {
        return tripMemberService.listMembers(principal.id(), tripId);
    }

    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse invite(@AuthenticationPrincipal UserPrincipal principal,
                                 @PathVariable Long tripId,
                                 @Valid @RequestBody InviteRequest request) {
        return tripMemberService.invite(principal.id(), tripId, request);
    }

    @PatchMapping("/members/{userId}")
    public MemberResponse changeRole(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long tripId,
                                     @PathVariable Long userId,
                                     @Valid @RequestBody ChangeRoleRequest request) {
        return tripMemberService.changeRole(principal.id(), tripId, userId, request);
    }

    @DeleteMapping("/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal UserPrincipal principal,
                             @PathVariable Long tripId,
                             @PathVariable Long userId) {
        tripMemberService.removeMember(principal.id(), tripId, userId);
    }

    @GetMapping("/invitations")
    public List<InvitationResponse> listInvitations(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable Long tripId) {
        return tripMemberService.listPendingInvitations(principal.id(), tripId);
    }

    @DeleteMapping("/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvitation(@AuthenticationPrincipal UserPrincipal principal,
                                 @PathVariable Long tripId,
                                 @PathVariable Long invitationId) {
        tripMemberService.revokeInvitation(principal.id(), tripId, invitationId);
    }
}
