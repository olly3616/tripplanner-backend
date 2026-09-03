package com.voyage.global.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Plain unit test (no Spring/Docker) so the build always has an executed test,
 * even on machines without Docker.
 */
class ErrorResponseTest {

    @Test
    void of_copiesCodeAndMessageFromErrorCode() {
        ErrorResponse response = ErrorResponse.of(ErrorCode.NOT_FOUND);

        assertEquals("C004", response.code());
        assertEquals(ErrorCode.NOT_FOUND.getMessage(), response.message());
        assertTrue(response.errors().isEmpty());
        assertNotNull(response.timestamp());
    }

    @Test
    void of_withFieldErrors_keepsThem() {
        List<ErrorResponse.FieldError> fieldErrors =
                List.of(new ErrorResponse.FieldError("endsOn", "종료일은 시작일 이후여야 합니다."));

        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT, fieldErrors);

        assertEquals("C001", response.code());
        assertEquals(1, response.errors().size());
        assertEquals("endsOn", response.errors().get(0).field());
    }
}
