package com.simpleboard.board.board.application.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.simpleboard.board.board.application.dto.request.CommentListQuery;
import com.simpleboard.board.board.application.dto.response.CommentListQueryResult;
import com.simpleboard.board.board.application.query.CommentListCriteria;
import com.simpleboard.board.board.application.query.CommentListReadModel;
import com.simpleboard.board.board.domain.comment.vo.CommentState;
import com.simpleboard.board.board.domain.comment.vo.CommentType;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommentQueryServiceRepoConverterTest {

  CommentQueryServiceRepoConverter converter = new CommentQueryServiceRepoConverter();

  private CommentListReadModel.CommentSummary rmSummary(
      long id,
      Long parentId,
      CommentType type,
      String guestNickname,
      Long writerId,
      String content) {
    return CommentListReadModel.CommentSummary.builder()
        .commentState(CommentState.ACTIVATE)
        .commentId(id)
        .parentId(parentId)
        .commentType(type)
        .content(content)
        .nickname(guestNickname)
        .writerId(writerId)
        .createdAt(LocalDateTime.parse("2025-02-01T10:00:00"))
        .updatedAt(LocalDateTime.parse("2025-02-01T10:00:00"))
        .siblingSeq(1)
        .depth(0)
        .build();
  }

  private CommentListReadModel readModel(List<CommentListReadModel.CommentSummary> rows) {
    return CommentListReadModel.builder()
        .activeComments(79L)
        .currentPage(1)
        .totalPages(4)
        .nextPage(2)
        .size(10)
        .comments(rows)
        .build();
  }

  @Nested
  @DisplayName("getCriteria()")
  class GetCriteria {
    @Test
    @DisplayName("Query → Criteria 필드 일대일 매핑")
    void mapsQueryToCriteria() {
      CommentListQuery query = CommentListQuery.builder().postId(300L).page(3).size(20).build();

      CommentListCriteria c = converter.getCriteria(query);
      assertThat(c.postId()).isEqualTo(300L);
      assertThat(c.page()).isEqualTo(3);
      assertThat(c.size()).isEqualTo(20);
    }
  }

  @Nested
  @DisplayName("getQueryResult()")
  class GetQueryResult {

    @Test
    @DisplayName("GUEST는 원래 nickname 그대로, MEMBER는 nicknameMap을 사용")
    void guestVsMemberNicknameSource() {
      var rows =
          List.of(
              rmSummary(1L, 0L, CommentType.GUEST, "guest-alice", null, "g1"),
              rmSummary(2L, 0L, CommentType.MEMBER, null, 701L, "m1"));
      var rm = readModel(rows);

      Map<Long, String> nicknameMap = new LinkedHashMap<>();
      nicknameMap.put(701L, "member-alice");

      CommentListQueryResult out = converter.getQueryResult(rm, nicknameMap);

      assertThat(out.activeComments()).isEqualTo(79L);
      assertThat(out.totalPages()).isEqualTo(4);
      assertThat(out.currentPage()).isEqualTo(1);
      assertThat(out.nextPage()).isEqualTo(2);
      assertThat(out.size()).isEqualTo(10);

      assertThat(out.comments()).hasSize(2);
      assertThat(out.comments().get(0).commentId()).isEqualTo(1L);
      assertThat(out.comments().get(0).commentType()).isEqualTo(CommentType.GUEST);
      assertThat(out.comments().get(0).nickname()).isEqualTo("guest-alice"); // GUEST → 원본 유지

      assertThat(out.comments().get(1).commentId()).isEqualTo(2L);
      assertThat(out.comments().get(1).commentType()).isEqualTo(CommentType.MEMBER);
      assertThat(out.comments().get(1).nickname()).isEqualTo("member-alice"); // MEMBER → map 적용
    }

    @Test
    @DisplayName("nicknameMap에 없는 MEMBER writerId → null 반환(현재 구현)")
    void missingMemberNicknameBecomesNull() {
      var rows = List.of(rmSummary(3L, 0L, CommentType.MEMBER, null, 999L, "m-unknown"));
      var rm = readModel(rows);

      CommentListQueryResult out = converter.getQueryResult(rm, Map.of());
      assertThat(out.comments()).hasSize(1);
      assertThat(out.comments().get(0).nickname()).isNull();
    }

    @Test
    @DisplayName("입력 순서 보존 및 공통 메타데이터 보존")
    void preserveOrderAndMeta() {
      var rows =
          List.of(
              rmSummary(10L, 0L, CommentType.GUEST, "g1", null, "x"),
              rmSummary(11L, 0L, CommentType.GUEST, "g2", null, "y"));
      var rm = readModel(rows);

      CommentListQueryResult out = converter.getQueryResult(rm, Map.of());
      assertThat(out.comments())
          .extracting(CommentListQueryResult.CommentSummary::commentId)
          .containsExactly(10L, 11L);
      assertThat(out.activeComments()).isEqualTo(79L);
      assertThat(out.size()).isEqualTo(10);
    }
  }
}
