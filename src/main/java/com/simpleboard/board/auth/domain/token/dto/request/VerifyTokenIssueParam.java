package com.simpleboard.board.auth.domain.token.dto.request;

import com.simpleboard.board.auth.domain.token.vo.VerifyPurpose;
import java.time.Duration;

/**
 * <b>VerifyTokenIssueCommand</b> Request DTO.
 *
 * <p>이메일/닉네임/비밀번호 검증을 위한 Verify 토큰 발급 파라미터
 *
 * @domain request-dto
 * @since 1.0
 */
public record VerifyTokenIssueParam(VerifyPurpose purpose, String subject, Duration ttl) {}
