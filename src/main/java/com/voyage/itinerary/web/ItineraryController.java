package com.voyage.itinerary.web;

import com.voyage.auth.security.UserPrincipal;
import com.voyage.itinerary.dto.CreateItineraryItemRequest;
import com.voyage.itinerary.dto.ItineraryItemResponse;
import com.voyage.itinerary.service.ItineraryService;
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
@RequestMapping("/api/trips/{tripId}/itinerary")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;

    @GetMapping
    public List<ItineraryItemResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable Long tripId) {
        return itineraryService.list(principal.id(), tripId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItineraryItemResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long tripId,
                                        @Valid @RequestBody CreateItineraryItemRequest request) {
        return itineraryService.create(principal.id(), tripId, request);
    }
}
