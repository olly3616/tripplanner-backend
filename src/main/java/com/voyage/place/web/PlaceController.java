package com.voyage.place.web;

import com.voyage.auth.security.UserPrincipal;
import com.voyage.place.domain.PlaceStatus;
import com.voyage.place.dto.PlaceResponse;
import com.voyage.place.dto.PlaceSearchResponse;
import com.voyage.place.dto.SavePlaceRequest;
import com.voyage.place.dto.UpdatePlaceRequest;
import com.voyage.place.service.PlaceService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping("/search")
    public List<PlaceSearchResponse> search(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable Long tripId,
                                            @RequestParam String query) {
        return placeService.search(principal.id(), tripId, query);
    }

    @GetMapping
    public List<PlaceResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long tripId,
                                    @RequestParam(required = false) PlaceStatus status,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) String tag) {
        return placeService.list(principal.id(), tripId, status, category, tag);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaceResponse save(@AuthenticationPrincipal UserPrincipal principal,
                              @PathVariable Long tripId,
                              @Valid @RequestBody SavePlaceRequest request) {
        return placeService.save(principal.id(), tripId, request);
    }

    @PatchMapping("/{placeId}")
    public PlaceResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                @PathVariable Long tripId,
                                @PathVariable Long placeId,
                                @Valid @RequestBody UpdatePlaceRequest request) {
        return placeService.update(principal.id(), tripId, placeId, request);
    }

    @DeleteMapping("/{placeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal,
                       @PathVariable Long tripId,
                       @PathVariable Long placeId) {
        placeService.delete(principal.id(), tripId, placeId);
    }
}
