package com.voyage.activity.web;

import com.voyage.activity.dto.ActivityResponse;
import com.voyage.activity.service.ActivityService;
import com.voyage.auth.security.UserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public List<ActivityResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long tripId) {
        return activityService.list(principal.id(), tripId);
    }
}
