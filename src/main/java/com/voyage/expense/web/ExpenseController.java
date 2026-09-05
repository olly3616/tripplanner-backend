package com.voyage.expense.web;

import com.voyage.auth.security.UserPrincipal;
import com.voyage.expense.dto.ExpenseRequest;
import com.voyage.expense.dto.ExpenseResponse;
import com.voyage.expense.service.ExpenseService;
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
@RequestMapping("/api/trips/{tripId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public List<ExpenseResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long tripId) {
        return expenseService.list(principal.id(), tripId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long tripId,
                                  @Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(principal.id(), tripId, request);
    }
}
