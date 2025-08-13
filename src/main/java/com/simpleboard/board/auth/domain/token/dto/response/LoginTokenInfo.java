package com.simpleboard.board.auth.domain.token.dto.response;

import com.simpleboard.board.auth.domain.common.vo.Role;
import com.simpleboard.board.auth.domain.token.vo.TokenPurpose;
import java.time.Instant;
import lombok.Builder;

@Builder
public record LoginTokenInfo(
    TokenPurpose tokenPurpose,
    Long memberId,
    Role role,
    String audience,
    String issuer,
    Instant issueAt,
    Instant expiredAt) {}
