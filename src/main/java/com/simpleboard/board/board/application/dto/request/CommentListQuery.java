package com.simpleboard.board.board.application.dto.request;

import lombok.Builder;

/**
 * <b>Comment 목록 조회 요청 모델</b>
 *
 * <p>Req: presentation -> application
 *
 * @domain request-dto
 */
@Builder
public record CommentListQuery(Long postId, int page, int size) {}
