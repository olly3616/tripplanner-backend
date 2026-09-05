package com.voyage.expense.web;

import com.voyage.auth.security.UserPrincipal;
import com.voyage.expense.dto.ExpenseResponse;
import com.voyage.expense.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/expenses/{expenseId}/receipt")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExpenseResponse upload(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long expenseId,
                                  @RequestParam("file") MultipartFile file) {
        return receiptService.attachReceipt(principal.id(), expenseId, file);
    }
}
