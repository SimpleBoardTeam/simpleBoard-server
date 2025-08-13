package com.simpleboard.board.auth.domain.token.exception;

import com.simpleboard.board.global.exception.ErrorCode;
import com.simpleboard.board.global.exception.ServiceException;

public class TokenUserBlockedException extends ServiceException {
  private static final ErrorCode ERROR_CODE = ErrorCode.TOKEN_BLOCKED;

  public TokenUserBlockedException() {
    super(ERROR_CODE);
  }
}
