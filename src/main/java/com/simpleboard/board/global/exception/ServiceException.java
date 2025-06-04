package com.simpleboard.board.global.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    private final ErrorCode errorCode;

    protected ServiceException(ErrorCode errorCode, String customMsg) {
        super(customMsg == null ? errorCode.getMessage() : customMsg);
        this.errorCode = errorCode;
    }

    /* 정적 팩토리 method 패턴 */
    public static ServiceException of(ErrorCode code) {
        return new ServiceException(code, null);
    }
    public static ServiceException of(ErrorCode code, String msg) {
        return new ServiceException(code, msg);
    }
}
