package com.simpleboard.board.auth.domain.token.dto.request;

import lombok.Builder;

/**
 * <b>RefreshTokenRotationCommand</b> Request DTO.
 *
 * <p>리프레시 토큰 로테이션 시 필요한 파라미터 모델
 *
 * @domain request-dto
 * @since 1.0
 */
@Builder
public record RefreshTokenRotationParam(String oldRefreshRaw) {}
