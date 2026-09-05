package com.voyage.poll.web;

import com.voyage.auth.security.UserPrincipal;
import com.voyage.poll.dto.PollResponse;
import com.voyage.poll.dto.VoteRequest;
import com.voyage.poll.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollItemController {

    private final PollService pollService;

    @GetMapping("/{pollId}")
    public PollResponse get(@AuthenticationPrincipal UserPrincipal principal,
                            @PathVariable Long pollId) {
        return pollService.get(principal.id(), pollId);
    }

    @PostMapping("/{pollId}/vote")
    public PollResponse vote(@AuthenticationPrincipal UserPrincipal principal,
                             @PathVariable Long pollId,
                             @Valid @RequestBody VoteRequest request) {
        return pollService.vote(principal.id(), pollId, request);
    }

    @DeleteMapping("/{pollId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal,
                       @PathVariable Long pollId) {
        pollService.delete(principal.id(), pollId);
    }
}
