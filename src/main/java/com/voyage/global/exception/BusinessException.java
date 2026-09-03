package com.voyage.global.exception;

import lombok.Getter;

/**
 * Base type for expected, domain-level failures. Carries an {@link ErrorCode}
 * so {@link GlobalExceptionHandler} can translate it into a consistent HTTP response.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
