package com.voyage.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Application-wide error catalog. Each entry maps to an HTTP status, a stable
 * machine-readable code (used by clients) and a default human message.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C002", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C003", "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "C005", "다른 변경 사항과 충돌했습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C006", "허용되지 않은 메서드입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 오류가 발생했습니다."),

    // Auth
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "A001", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않은 토큰입니다."),

    // Members & invitations
    INVITATION_INVALID(HttpStatus.BAD_REQUEST, "M001", "유효하지 않거나 만료된 초대입니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "M002", "허용되지 않은 역할입니다."),
    CANNOT_MODIFY_OWNER(HttpStatus.FORBIDDEN, "M003", "소유자는 변경하거나 제거할 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M004", "멤버를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
