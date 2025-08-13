package com.simpleboard.board.auth.domain.token.dto.request;

import com.simpleboard.board.auth.domain.common.vo.Role;
import lombok.Builder;

/**
 * <b>LoginTokenIssueCommand</b> Request DTO.
 *
 * <p>로그인 성공 후 Access/Refresh 토큰 발급에 필요한 파라미터 모델
 *
 * @domain request-dto
 * @since 1.0
 */
@Builder
public record LoginTokenIssueParam(Long memberId, Role role) {}
