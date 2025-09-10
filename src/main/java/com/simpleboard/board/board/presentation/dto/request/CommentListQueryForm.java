package com.simpleboard.board.board.presentation.dto.request;

import lombok.Builder;

/**
 * Comment 목록 조회 요청 모델.
 *
 * <p>Query 경로로의 요청 수행
 *
 * <p>Req: Client -> Presentation
 *
 * @domain request-dto
 */
@Builder
public record CommentListQueryForm(int page, int size) {}
