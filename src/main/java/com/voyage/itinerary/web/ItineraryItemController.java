package com.voyage.itinerary.web;

import com.voyage.auth.security.UserPrincipal;
import com.voyage.itinerary.dto.ItineraryItemResponse;
import com.voyage.itinerary.dto.ReorderRequest;
import com.voyage.itinerary.dto.UpdateItineraryItemRequest;
import com.voyage.itinerary.service.ItineraryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Item-scoped itinerary operations. The trip (and thus authorization) is
 * resolved from the item itself.
 */
@RestController
@RequestMapping("/api/itinerary")
@RequiredArgsConstructor
public class ItineraryItemController {

    private final ItineraryService itineraryService;

    @PatchMapping("/{itemId}")
    public ItineraryItemResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long itemId,
                                        @Valid @RequestBody UpdateItineraryItemRequest request) {
        return itineraryService.update(principal.id(), itemId, request);
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal,
                       @PathVariable Long itemId) {
        itineraryService.delete(principal.id(), itemId);
    }

    @PostMapping("/reorder")
    public List<ItineraryItemResponse> reorder(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody ReorderRequest request) {
        return itineraryService.reorder(principal.id(), request);
    }
}
