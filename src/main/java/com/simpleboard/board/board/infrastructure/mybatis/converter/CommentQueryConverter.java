package com.simpleboard.board.board.infrastructure.mybatis.converter;

import com.simpleboard.board.board.application.query.CommentListCriteria;
import com.simpleboard.board.board.application.query.CommentListReadModel;
import com.simpleboard.board.board.domain.comment.vo.CommentState;
import com.simpleboard.board.board.domain.comment.vo.CommentType;
import com.simpleboard.board.board.infrastructure.mybatis.mapper.CommentListCondition;
import com.simpleboard.board.board.infrastructure.mybatis.mapper.CommentSummaryData;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * <b>Repository <-> Mapper converter</b>
 *
 * <ul>
 *   <li>Criteria -> Condition 으로 변환
 *   <li>Data -> ReadModel 으로 변환
 *   <li>0-based <-> 1-based 페이징 변환을 담당</->
 * </ul>
 */
@Component
public class CommentQueryConverter {

  public CommentListCondition toCondition(CommentListCriteria criteria) {
    int limit = criteria.size() != 0 ? criteria.size() : 10;
    int zeroBasedPage = criteria.page() > 0 ? criteria.page() - 1 : 0;
    return CommentListCondition.builder()
        .postId(criteria.postId())
        .limit(limit)
        .offset(zeroBasedPage * limit)
        .build();
  }

  /**
   * <b>응답 반환 DTO</b>
   *
   * <ul>
   *   <li>페이지 관련 정보 처리
   *   <li>Enum type 매핑
   * </ul>
   *
   * @since 1.0
   */
  public CommentListReadModel toReadModel(
      List<CommentSummaryData> rows,
      Long activeComments,
      Long rootComments,
      CommentListCondition condition) {

    int totalPages =
        (int) (rootComments / condition.limit()) + (rootComments % condition.limit() == 0 ? 0 : 1);
    int oneBasedCurPage = condition.offset() / condition.limit() + 1;

    return CommentListReadModel.builder()
        .activeComments(activeComments)
        .totalPages(totalPages)
        .currentPage(oneBasedCurPage)
        .nextPage(oneBasedCurPage + 1 < totalPages ? oneBasedCurPage + 1 : totalPages)
        .size(condition.limit())
        .comments(
            rows.stream()
                .map(
                    row ->
                        CommentListReadModel.CommentSummary.builder()
                            .commentState(CommentState.valueOf(row.commentState()))
                            .commentId(row.commentId())
                            .parentId(row.parentId())
                            .commentType(CommentType.valueOf(row.commentType().split("_")[0]))
                            .content(row.content())
                            .nickname(row.nickname())
                            .writerId(row.writerId())
                            .createdAt(row.createdAt())
                            .updatedAt(row.updatedAt())
                            .siblingSeq(row.siblingSeq())
                            .depth(row.depth())
                            .build())
                .toList())
        .build();
  }
}
