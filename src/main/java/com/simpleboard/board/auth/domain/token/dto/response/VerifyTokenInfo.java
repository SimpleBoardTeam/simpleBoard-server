package com.simpleboard.board.auth.domain.token.dto.response;

import com.simpleboard.board.auth.domain.token.vo.VerifyPurpose;
import java.time.Instant;
import lombok.Builder;

@Builder
public record VerifyTokenInfo(
    VerifyPurpose verifyPurpose,
    String subject, // verify 대상(email, nickname 등)
    String audience,
    String issuer,
    Instant issueAt,
    Instant expiredAt) {}
