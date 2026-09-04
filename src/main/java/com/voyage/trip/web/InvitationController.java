package com.voyage.trip.web;

import com.voyage.auth.security.UserPrincipal;
import com.voyage.trip.dto.AcceptInvitationRequest;
import com.voyage.trip.dto.AcceptInvitationResponse;
import com.voyage.trip.service.TripMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final TripMemberService tripMemberService;

    /** Accepts an invitation for the currently authenticated user via its token. */
    @PostMapping("/accept")
    public AcceptInvitationResponse accept(@AuthenticationPrincipal UserPrincipal principal,
                                           @Valid @RequestBody AcceptInvitationRequest request) {
        return tripMemberService.accept(principal.id(), request.token());
    }
}
