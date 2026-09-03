package com.voyage.trip.web;

import com.voyage.auth.security.UserPrincipal;
import com.voyage.trip.domain.TripStatus;
import com.voyage.trip.dto.ChangeTripStatusRequest;
import com.voyage.trip.dto.CreateTripRequest;
import com.voyage.trip.dto.TripResponse;
import com.voyage.trip.dto.UpdateTripRequest;
import com.voyage.trip.service.TripService;
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
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse create(@AuthenticationPrincipal UserPrincipal principal,
                               @Valid @RequestBody CreateTripRequest request) {
        return tripService.create(principal.id(), request);
    }

    @GetMapping
    public List<TripResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                   @RequestParam(required = false) TripStatus status) {
        return tripService.listMyTrips(principal.id(), status);
    }

    @GetMapping("/{tripId}")
    public TripResponse get(@AuthenticationPrincipal UserPrincipal principal,
                            @PathVariable Long tripId) {
        return tripService.get(principal.id(), tripId);
    }

    @PatchMapping("/{tripId}")
    public TripResponse update(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable Long tripId,
                               @Valid @RequestBody UpdateTripRequest request) {
        return tripService.update(principal.id(), tripId, request);
    }

    @PatchMapping("/{tripId}/status")
    public TripResponse changeStatus(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long tripId,
                                     @Valid @RequestBody ChangeTripStatusRequest request) {
        return tripService.changeStatus(principal.id(), tripId, request.status());
    }

    @DeleteMapping("/{tripId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal,
                       @PathVariable Long tripId) {
        tripService.delete(principal.id(), tripId);
    }
}
