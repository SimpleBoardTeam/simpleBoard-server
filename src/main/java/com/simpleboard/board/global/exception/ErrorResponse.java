package com.simpleboard.board.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 *  <h5>에러 응답 클래스</h5>
 *  <li>record + static factory 로 불변 응답 캡슐</li>
 *  <li>timestamp·path·requestId 포함</li>
 *  <li><b>fieldErrors</b>는 Validation 에러일 때만 사용</li>
 */
public record ErrorResponse(
        String              code,
        String              message,
        int                 status,
        String              path,
        String              requestId,
        LocalDateTime timestamp,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<FieldError> fieldErrors
) {

    /* Validation 오류용 필드*/
    public record FieldError(String field, String reason) {}

    /* 일반 예외용 */
    public static ErrorResponse of(ErrorCode e, HttpServletRequest req, String requestId) {
        return new ErrorResponse(
                e.getCode(),
                e.getMessage(),
                e.getStatus().value(),
                req.getRequestURI(),
                requestId,
                LocalDateTime.now(),
                List.of()
        );
    }

    /* Validation 실패용 */
    public static ErrorResponse ofValidation(ErrorCode e, HttpServletRequest req,
                                             String requestId, List<FieldError> errors) {
        return new ErrorResponse(
                e.getCode(), e.getMessage(), e.getStatus().value(),
                req.getRequestURI(), requestId, LocalDateTime.now(), errors
        );
    }
}
