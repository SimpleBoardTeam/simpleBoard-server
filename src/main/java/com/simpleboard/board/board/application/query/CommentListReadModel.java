package com.simpleboard.board.board.application.query;

import com.simpleboard.board.board.domain.comment.vo.CommentState;
import com.simpleboard.board.board.domain.comment.vo.CommentType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

/**
 * Comment 목록 조회 반환 DTO
 *
 * <p>Res: service <- repository
 *
 * @domain response-dto
 */
@Builder
public record CommentListReadModel(
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
