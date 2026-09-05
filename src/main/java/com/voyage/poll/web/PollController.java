package com.voyage.poll.web;

import com.voyage.auth.security.UserPrincipal;
import com.voyage.poll.dto.CreatePollRequest;
import com.voyage.poll.dto.PollResponse;
import com.voyage.poll.service.PollService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @GetMapping
    public List<PollResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable Long tripId) {
        return pollService.list(principal.id(), tripId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PollResponse create(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable Long tripId,
                               @Valid @RequestBody CreatePollRequest request) {
        return pollService.create(principal.id(), tripId, request);
    }
}
