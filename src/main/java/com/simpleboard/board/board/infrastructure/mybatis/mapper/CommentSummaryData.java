package com.simpleboard.board.board.infrastructure.mybatis.mapper;

import java.time.LocalDateTime;
import lombok.Builder;

/**
 * <b>Comment 목록 조회 응답 모델</b>
 *
 * <p>단일 Comment에 대한 메타 정보를 담은 DTO
 *
 * <p>Res: repository <- mapper
 *
 * @domain response-dto
 */
@Builder
public record CommentSummaryData(
    String commentState,
    Long commentId,
    Long parentId,
    String commentType,
    String content,
    String nickname,
    Long writerId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Integer siblingSeq,
    Integer depth) {}
