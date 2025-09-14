package com.simpleboard.board.board.infrastructure.mybatis.mapper;

import lombok.Builder;

/**
 * <b>Comment 목록 조회 Condition</b>
 *
 * <p>Req: repository -> mapper
 *
 * @domain request-dto
 */
@Builder
public record CommentListCondition(Long postId, int limit, int offset) {}
