package com.simpleboard.board.board.presentation.converter;

import com.simpleboard.board.board.application.dto.request.CommentListQuery;
import com.simpleboard.board.board.application.dto.response.CommentListQueryResult;
import com.simpleboard.board.board.domain.comment.vo.CommentState;
import com.simpleboard.board.board.presentation.dto.request.CommentListQueryForm;
import com.simpleboard.board.board.presentation.dto.response.CommentListQueryResponse;
import com.simpleboard.board.board.presentation.dto.response.CommentListQueryResponse.CommentSummary.CommentSummaryBuilder;
import org.springframework.stereotype.Component;

/**
 * <b>Post query presentation <-> application 컨버터</b>
 *
 * @version 1.0
 */
@Component
public class CommentQueryPresentationApplicationConverter {

  /**************************
   * Request converter
   * presentation -> Service
   **************************/

  public CommentListQuery toQuery(CommentListQueryForm form, Long postId) {
    return CommentListQuery.builder().postId(postId).page(form.page()).size(form.size()).build();
  }

  /**************************
   * Response converter
   * presentation <- Service
   **************************/

  public CommentListQueryResponse toQueryResponse(CommentListQueryResult result) {
    return CommentListQueryResponse.builder()
        .totalComments(result.activeComments())
        .page(result.nextPage())
        .size(result.size())
        .comments(
            result.comments().stream()
                .map(
                    summary -> {
                      CommentSummaryBuilder builder =
                          CommentListQueryResponse.CommentSummary.builder();
                      if (summary.commentState().equals(CommentState.ACTIVATE)) {
                        builder
                            .isDeleted(false)
                            .commentId(summary.commentId())
                            .parentId(summary.parentId())
                            .commentType(summary.commentType().toString())
                            .content(summary.content())
                            .nickname(summary.nickname())
                            .writerId(summary.writerId())
                            .createdAt(summary.createdAt())
                            .updatedAt(summary.updatedAt())
                            .siblingSeq(summary.siblingSeq())
                            .depth(summary.depth());
                      } else {
                        builder
                            .isDeleted(true)
                            .siblingSeq(summary.siblingSeq())
                            .depth(summary.depth());
                      }
                      return builder.build();
                    })
                .toList())
        .build();
  }
}
