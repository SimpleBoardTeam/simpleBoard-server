package com.simpleboard.board.board.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

/**
 * Comment 목록 조회 응답 모델.
 *
 * <p>Res: Client <- Presentation
 *
 * @domain response-dto
 */
@Builder
public record CommentListQueryResponse(
    long totalComments, int page, int size, List<CommentSummary> comments) {
  @Builder
  public record CommentSummary(
      Boolean isDeleted,
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
}
