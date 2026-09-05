package com.voyage.expense.web;

import com.voyage.auth.security.UserPrincipal;
import com.voyage.expense.dto.ExpenseRequest;
import com.voyage.expense.dto.ExpenseResponse;
import com.voyage.expense.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Item-scoped expense operations; the trip is resolved from the expense. */
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseItemController {

    private final ExpenseService expenseService;

    @PatchMapping("/{expenseId}")
    public ExpenseResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long expenseId,
                                  @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(principal.id(), expenseId, request);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal,
                       @PathVariable Long expenseId) {
        expenseService.delete(principal.id(), expenseId);
    }
}
