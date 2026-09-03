package com.voyage.global.exception;

import java.time.Instant;
import java.util.List;

/**
 * Standard error body returned by every failed request.
 *
 * <pre>
 * {
 *   "code": "C001",
 *   "message": "잘못된 입력입니다.",
 *   "errors": [ { "field": "endsOn", "reason": "종료일은 시작일 이후여야 합니다." } ],
 *   "timestamp": "2026-09-04T03:00:00Z"
 * }
 * </pre>
 */
public record ErrorResponse(
        String code,
        String message,
        List<FieldError> errors,
        Instant timestamp
) {

    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), List.of(), Instant.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, List.of(), Instant.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), errors, Instant.now());
    }
}
