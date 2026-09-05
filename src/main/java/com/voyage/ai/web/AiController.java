package com.voyage.ai.web;

import com.voyage.ai.dto.AiDraftRequest;
import com.voyage.ai.dto.ItineraryDraftResponse;
import com.voyage.ai.service.AiDraftService;
import com.voyage.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiDraftService aiDraftService;

    /** Returns an editable draft; it is never auto-committed to the itinerary. */
    @PostMapping("/itinerary-drafts")
    public ItineraryDraftResponse itineraryDraft(@AuthenticationPrincipal UserPrincipal principal,
                                                 @PathVariable Long tripId,
                                                 @Valid @RequestBody(required = false) AiDraftRequest request) {
        return aiDraftService.suggest(principal.id(), tripId, request);
    }
}
