package com.simpleboard.board.board.application.query;

import lombok.Builder;

/**
 * Comment 목록 조회 DTO
 *
 * <p>Req: service -> repository
 *
 * @domain request-dto
 */
@Builder
public record CommentListCriteria(Long postId, int page, int size) {}
