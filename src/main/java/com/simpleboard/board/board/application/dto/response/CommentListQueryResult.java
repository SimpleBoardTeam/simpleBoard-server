package com.simpleboard.board.board.application.dto.response;

import com.simpleboard.board.board.domain.comment.vo.CommentState;
import com.simpleboard.board.board.domain.comment.vo.CommentType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

/**
 * <b>Comment 목록 조회 응답 모델</b>
 *
 * <p>Res: presentation <- application
 *
 * @domain response-dto
 */
@Builder
public record CommentListQueryResult(
    long activeComments,
    int totalPages,
    int currentPage,
    int nextPage,
    int size,
    List<CommentSummary> comments) {
  @Builder
  public record CommentSummary(
      CommentState commentState,
      Long commentId,
      Long parentId,
      CommentType commentType,
      String content,
      String nickname,
      Long writerId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      Integer siblingSeq,
      Integer depth) {}
}
