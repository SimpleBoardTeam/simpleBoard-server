package com.simpleboard.board.auth.domain.token.util;

import com.simpleboard.board.auth.domain.token.vo.Token;
import com.simpleboard.board.auth.domain.token.vo.TokenClaims;
import java.util.Date;

/**
 * <b>TokenProvider</b> Port.
 *
 * <p>토큰 발급/검증/파싱을 위한 추상 포트
 *
 * @domain port
 * @since 1.0
 */
public interface TokenProvider {
  /**
   * <b>토큰 발급 메서드</b>
   *
   * @param claims 발급용 클레임
   * @return 발급된 토큰
   * @since 1.0
   */
  Token issueToken(TokenClaims claims);

  /**
   * <b>토큰 검증 및 파싱 메서드</b>
   *
   * <p>토큰의 만료시간에 대한 검증 후 파싱하여 반환
   *
   * @param token 토큰 원문
   * @return 파싱된 클레임
   * @since 1.0
   */
  TokenClaims verifyAndParseToken(String token, Date now);
}
