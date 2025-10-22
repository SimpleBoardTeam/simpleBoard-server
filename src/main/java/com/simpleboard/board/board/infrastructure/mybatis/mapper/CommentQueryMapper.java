package com.simpleboard.board.board.infrastructure.mybatis.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <b>Mybatis용 Mapper 클래스</b>
 *
 * <p>Comment 조회 요청을 받아 xml 파일에 정의되어 있는 SQL 쿼리 실행
 */
@Mapper
public interface CommentQueryMapper {
  List<CommentSummaryData> getCommentList(CommentListCondition condition);

  /**
   * <b>Active 상태의 Comment 수 조회</b>
   *
   * <ul>
   *   <li>postId의 Post에 작성된 Active 상태의 Comment의 총 수 반환
   *   <li>soft delete 상태의 Comment는 카운트하지 않음
   * </ul>
   *
   * @since 1.0
   */
  Long countActiveComments(@Param("postId") Long postId);

  /**
   * <b>Root Comment 수 조회</b>
   *
   * <ul>
   *   <li>postId의 Post에 작성된 Root Comment의 총 수 반환
   *   <li>부모가 없는, 최상의 Comment만 카운트
   *   <li>soft delete 상태의 Comment도 카운트
   * </ul>
   *
   * @since 1.0
   */
  Long countRootComments(@Param("postId") Long postId);
}
