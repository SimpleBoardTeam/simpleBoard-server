package com.simpleboard.board.board.infrastructure.mybatis.converter;

import static org.assertj.core.api.Assertions.*;

import com.simpleboard.board.board.application.query.CommentListReadModel;
import com.simpleboard.board.board.domain.comment.vo.CommentState;
import com.simpleboard.board.board.domain.comment.vo.CommentType;
import com.simpleboard.board.board.infrastructure.mybatis.mapper.CommentListCondition;
import com.simpleboard.board.board.infrastructure.mybatis.mapper.CommentSummaryData;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommentQueryConverterTest {

  CommentQueryConverter converter = new CommentQueryConverter();

  private CommentSummaryData row(
      long id, long parentId, String type, String state, String content) {
    return CommentSummaryData.builder()
        .commentId(id)
        .parentId(parentId)
        .commentType(type) // "GUEST_COMMENT" / "MEMBER_COMMENT"
        .commentState(state) // "ACTIVATE" 등
        .content(content)
        .nickname("alice")
        .writerId(701L)
        .createdAt(LocalDateTime.parse("2025-02-01T10:00:00"))
        .updatedAt(LocalDateTime.parse("2025-02-01T10:00:00"))
        .siblingSeq(1)
        .depth(0)
        .build();
  }

  @Nested
  @DisplayName("toCondition()")
  class ToCondition {

    @Test
    @DisplayName("size=0 → 기본 limit=10, page=0/음수 → offset=0")
    void defaultLimitAndPageNormalization() {
      var cond =
          converter.toCondition(
              com.simpleboard.board.board.application.query.CommentListCriteria.builder()
                  .postId(300L)
                  .size(0)
                  .page(0)
                  .build());

      assertThat(cond.limit()).isEqualTo(10);
      assertThat(cond.offset()).isEqualTo(0);
      assertThat(cond.postId()).isEqualTo(300L);

      var condNeg =
          converter.toCondition(
              com.simpleboard.board.board.application.query.CommentListCriteria.builder()
                  .postId(300L)
                  .size(0)
                  .page(-5)
                  .build());

      assertThat(condNeg.limit()).isEqualTo(10);
      assertThat(condNeg.offset()).isEqualTo(0);
    }

    @Test
    @DisplayName("page=3, size=20 → offset=(3-1)*20")
    void offsetCalc() {
      var cond =
          converter.toCondition(
              com.simpleboard.board.board.application.query.CommentListCriteria.builder()
                  .postId(300L)
                  .size(20)
                  .page(3)
                  .build());

      assertThat(cond.limit()).isEqualTo(20);
      assertThat(cond.offset()).isEqualTo(40);
    }
  }

  @Nested
  @DisplayName("toReadModel()")
  class ToReadModel {

    @Test
    @DisplayName("기본 매핑 + 페이지 산식 검증(totalPages/nextPage)")
    void mappingAndPagination() {
      var cond = CommentListCondition.builder().postId(300L).limit(10).offset(0).build();

      List<CommentSummaryData> rows =
          List.of(
              row(3001L, 0L, "MEMBER_COMMENT", "ACTIVATE", "root-1"),
              row(3002L, 0L, "GUEST_COMMENT", "ACTIVATE", "root-2"));

      long active = 79L;
      long root = 31L; // totalPages = ceil(31/10) = 4

      CommentListReadModel rm = converter.toReadModel(rows, active, root, cond);

      assertThat(rm.activeComments()).isEqualTo(79L);
      assertThat(rm.totalPages()).isEqualTo(4);
      assertThat(rm.currentPage()).isEqualTo(1);
      assertThat(rm.nextPage()).isEqualTo(2);
      assertThat(rm.size()).isEqualTo(10);
      assertThat(rm.comments()).hasSize(2);

      var c1 = rm.comments().get(0);
      assertThat(c1.commentId()).isEqualTo(3001L);
      assertThat(c1.commentType()).isEqualTo(CommentType.MEMBER);
      assertThat(c1.commentState()).isEqualTo(CommentState.valueOf("ACTIVATE"));
      assertThat(c1.content()).isEqualTo("root-1");

      var c2 = rm.comments().get(1);
      assertThat(c2.commentType()).isEqualTo(CommentType.GUEST);
    }

    @Test
    @DisplayName("마지막 페이지에서 nextPage는 totalPages 초과 금지")
    void lastPageNextClamped() {
      var cond =
          CommentListCondition.builder().postId(300L).limit(10).offset(30).build(); // 4페이지(1-based)
      long root = 31L; // totalPages=4

      CommentListReadModel rm = converter.toReadModel(List.of(), 79L, root, cond);
      assertThat(rm.currentPage()).isEqualTo(4);
      assertThat(rm.totalPages()).isEqualTo(4);
      assertThat(rm.nextPage()).isEqualTo(4);
    }

    @Test
    @DisplayName("rows=empty & rootComments=0인 경우(현 구현): totalPages=0, nextPage=0")
    void zeroRootCommentsEdgeCase() {
      var cond = CommentListCondition.builder().postId(300L).limit(10).offset(0).build();
      CommentListReadModel rm = converter.toReadModel(List.of(), 0L, 0L, cond);

      // 현재 구현 로직의 결과를 '그대로' 검증
      assertThat(rm.totalPages()).isEqualTo(0);
      assertThat(rm.currentPage()).isEqualTo(1);
      assertThat(rm.nextPage()).isEqualTo(0);
      assertThat(rm.comments()).isEmpty();
    }

    @Test
    @DisplayName("잘못된 commentType 문자열 → Enum 변환 실패(예외)")
    void invalidEnumFails() {
      var bad = row(1L, 0L, "UNKNOWN_KIND", "ACTIVATE", "x");
      var cond = CommentListCondition.builder().postId(300L).limit(10).offset(0).build();

      assertThatThrownBy(() -> converter.toReadModel(List.of(bad), 0L, 1L, cond))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
