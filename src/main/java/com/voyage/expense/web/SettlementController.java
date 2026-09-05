package com.voyage.expense.web;

import com.voyage.auth.security.UserPrincipal;
import com.voyage.expense.dto.SettlementResponse;
import com.voyage.expense.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public SettlementResponse settlement(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long tripId) {
        return settlementService.settle(principal.id(), tripId);
    }
}
