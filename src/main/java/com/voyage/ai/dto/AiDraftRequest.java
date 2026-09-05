package com.voyage.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import java.util.List;

/** Optional preferences for the draft. */
public record AiDraftRequest(
        List<String> preferredCategories,
        @Positive @Max(10) Integer itemsPerDay
) {
}
