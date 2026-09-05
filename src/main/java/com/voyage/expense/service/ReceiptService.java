package com.voyage.expense.service;

import com.voyage.expense.domain.Expense;
import com.voyage.expense.dto.ExpenseResponse;
import com.voyage.expense.repository.ExpenseRepository;
import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.storage.FileStorage;
import com.voyage.trip.domain.TripRole;
import com.voyage.trip.service.TripAccessGuard;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 5 * 1024 * 1024;

    private final ExpenseRepository expenseRepository;
    private final TripAccessGuard tripAccessGuard;
    private final FileStorage fileStorage;

    @Transactional
    public ExpenseResponse attachReceipt(Long userId, Long expenseId, MultipartFile file) {
        Expense expense = expenseRepository.findByIdWithSplits(expenseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        tripAccessGuard.requireAnyRole(expense.getTripId(), userId, TripRole.OWNER, TripRole.EDITOR);
        validate(file);

        FileStorage.StoredFile stored = fileStorage.store(readBytes(file), file.getContentType(),
                file.getOriginalFilename());
        expense.attachReceipt(stored.url());
        return ExpenseResponse.from(expense);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()
                || file.getSize() > MAX_BYTES
                || file.getContentType() == null
                || !ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
    }
}
