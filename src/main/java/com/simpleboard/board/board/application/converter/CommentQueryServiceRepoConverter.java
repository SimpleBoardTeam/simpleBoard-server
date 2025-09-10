package com.simpleboard.board.board.application.converter;

import com.simpleboard.board.board.application.dto.request.CommentListQuery;
import com.simpleboard.board.board.application.dto.response.CommentListQueryResult;
import com.simpleboard.board.board.application.query.CommentListCriteria;
import com.simpleboard.board.board.application.query.CommentListReadModel;
import com.simpleboard.board.board.domain.comment.vo.CommentType;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * <b>Comment query service <-> repository 컨버터</b>
 *
 * <ul>
 *   <li>Member Comment의 writer nickname 매핑 수행
 * </ul>
 *
 * @version 1.0
 */
@Component
public class CommentQueryServiceRepoConverter {

  /**************************
   * Request converter
   * Service -> Repository
   **************************/

  public CommentListCriteria getCriteria(CommentListQuery query) {
    return CommentListCriteria.builder()
        .postId(query.postId())
        .page(query.page())
        .size(query.size())
        .build();
  }

  /**************************
   * Response converter
   * Service <- Repository
   **************************/

  /**
   * <b>Comment 응답 컨버터 메서드</b>
   *
   * <p>Member Comment의 nickname을 삽입
   *
   * @since 1.0
   */
  public CommentListQueryResult getQueryResult(
      CommentListReadModel readModel, Map<Long, String> nicknameMap) {
    return CommentListQueryResult.builder()
        .activeComments(readModel.activeComments())
        .currentPage(readModel.currentPage())
        .totalPages(readModel.totalPages())
        .nextPage(readModel.nextPage())
        .size(readModel.size())
        .comments(
            readModel.comments().stream()
                .map(
                    (summary) ->
                        CommentListQueryResult.CommentSummary.builder()
                            .commentState(summary.commentState())
                            .commentId(summary.commentId())
                            .parentId(summary.parentId())
                            .commentType(summary.commentType())
                            .content(summary.content())
                            .nickname(
                                summary.commentType().equals(CommentType.GUEST)
                                    ? summary.nickname()
                                    : nicknameMap.get(summary.writerId()))
                            .writerId(summary.writerId())
                            .createdAt(summary.createdAt())
                            .updatedAt(summary.updatedAt())
                            .siblingSeq(summary.siblingSeq())
                            .depth(summary.depth())
                            .build())
                .toList())
        .build();
  }
}
