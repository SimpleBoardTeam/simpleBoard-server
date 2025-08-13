package com.simpleboard.board.auth.domain.token.vo;

import java.time.Instant;
import lombok.Builder;

/**
 * <b>Token</b> Value Object.
 *
 * <p>발급된 토큰의 원문과 만료 시각 보관
 *
 * @domain value-object
 * @since 1.0
 */
@Builder
public record Token(String raw, Instant expiredAt) {}
