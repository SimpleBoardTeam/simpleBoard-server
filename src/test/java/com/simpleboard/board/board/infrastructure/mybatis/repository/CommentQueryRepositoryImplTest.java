package com.simpleboard.board.board.infrastructure.mybatis.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.simpleboard.board.board.application.query.CommentListCriteria;
import com.simpleboard.board.board.application.query.CommentListReadModel;
import com.simpleboard.board.board.infrastructure.mybatis.converter.CommentQueryConverter;
import com.simpleboard.board.board.infrastructure.mybatis.mapper.CommentListCondition;
import com.simpleboard.board.board.infrastructure.mybatis.mapper.CommentQueryMapper;
import com.simpleboard.board.board.infrastructure.mybatis.mapper.CommentSummaryData;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CommentQueryRepositoryImplUnitTest {

  private final CommentQueryMapper mapper = mock(CommentQueryMapper.class);
  private final CommentQueryConverter converter = new CommentQueryConverter();
  private final CommentQueryRepositoryImpl repository =
      new CommentQueryRepositoryImpl(mapper, converter);

  private CommentSummaryData row(
      long id, long parentId, String type, String state, String content) {
    return CommentSummaryData.builder()
        .commentId(id)
        .parentId(parentId)
        .commentType(type)
        .commentState(state)
        .content(content)
        .nickname("alice")
        .writerId(701L)
        .createdAt(LocalDateTime.parse("2025-02-01T10:00:00"))
        .updatedAt(LocalDateTime.parse("2025-02-01T10:00:00"))
        .siblingSeq(1)
        .depth(0)
        .build();
  }

  @Test
  @DisplayName("page=0(첫 페이지) → Mapper에게 limit=10, offset=0 전달 + 페이지 메타 정합")
  void firstPage_conditionAndMeta() {
    // given
    List<CommentSummaryData> rows =
        List.of(
            row(3001L, 0L, "MEMBER_COMMENT", "ACTIVATE", "root-1"),
            row(3002L, 0L, "GUEST_COMMENT", "ACTIVATE", "root-2"));
    when(mapper.getCommentList(any())).thenReturn(rows);
    when(mapper.countRootComments(300L)).thenReturn(31L);
    when(mapper.countActiveComments(300L)).thenReturn(79L);

    // when
    CommentListReadModel rm =
        repository.getCommentList(
            CommentListCriteria.builder().postId(300L).size(10).page(0).build());

    // then: Mapper로 넘어간 Condition 검증
    var captor = ArgumentCaptor.forClass(CommentListCondition.class);
    verify(mapper, times(1)).getCommentList(captor.capture());
    CommentListCondition used = captor.getValue();
    assertThat(used.postId()).isEqualTo(300L);
    assertThat(used.limit()).isEqualTo(10);
    assertThat(used.offset()).isEqualTo(0);

    verify(mapper).countRootComments(300L);
    verify(mapper).countActiveComments(300L);
    verifyNoMoreInteractions(mapper);

    // then: 페이지 메타/데이터 검증
    assertThat(rm.activeComments()).isEqualTo(79L);
    assertThat(rm.totalPages()).isEqualTo(4);
    assertThat(rm.currentPage()).isEqualTo(1);
    assertThat(rm.nextPage()).isEqualTo(2);
    assertThat(rm.comments()).hasSize(2);
    assertThat(rm.comments())
        .extracting(CommentListReadModel.CommentSummary::commentId)
        .containsExactly(3001L, 3002L); // 입력 순서 보존
  }

  @Test
  @DisplayName("마지막 페이지(page=4) → nextPage는 totalPages 초과 금지")
  void lastPage_clampedNext() {
    when(mapper.getCommentList(any())).thenReturn(List.of());
    when(mapper.countRootComments(300L)).thenReturn(31L); // totalPages=4
    when(mapper.countActiveComments(300L)).thenReturn(79L);

    CommentListReadModel rm =
        repository.getCommentList(
            CommentListCriteria.builder().postId(300L).size(10).page(4).build());

    var captor = ArgumentCaptor.forClass(CommentListCondition.class);
    verify(mapper).getCommentList(captor.capture());
    assertThat(captor.getValue().offset()).isEqualTo(30); // (4-1)*10

    assertThat(rm.currentPage()).isEqualTo(4);
    assertThat(rm.totalPages()).isEqualTo(4);
    assertThat(rm.nextPage()).isEqualTo(4);
  }

  @Test
  @DisplayName("size=0 → 기본 limit=10으로 정규화된 Condition이 Mapper에 전달")
  void defaultLimitCondition() {
    when(mapper.getCommentList(any())).thenReturn(List.of());
    when(mapper.countRootComments(300L)).thenReturn(0L);
    when(mapper.countActiveComments(300L)).thenReturn(0L);

    repository.getCommentList(CommentListCriteria.builder().postId(300L).size(0).page(0).build());

    var captor = ArgumentCaptor.forClass(CommentListCondition.class);
    verify(mapper).getCommentList(captor.capture());
    assertThat(captor.getValue().limit()).isEqualTo(10);
    assertThat(captor.getValue().offset()).isEqualTo(0);
  }

  @Test
  @DisplayName("page가 매우 커도(현재 구현) currentPage는 offset 기반으로 커질 수 있음(상위 계층에서 유효성 검증 권장)")
  void pageOverflow_currentPageMayExceedTotal() {
    when(mapper.getCommentList(any())).thenReturn(List.of());
    when(mapper.countRootComments(300L)).thenReturn(10L); // totalPages=1 (limit=10)
    when(mapper.countActiveComments(300L)).thenReturn(10L);

    CommentListReadModel rm =
        repository.getCommentList(
            CommentListCriteria.builder().postId(300L).size(10).page(999).build());

    // currentPage는 999로 계산될 수 있음(현 구현), nextPage는 totalPages로 클램프
    assertThat(rm.currentPage()).isEqualTo(999);
    assertThat(rm.totalPages()).isEqualTo(1);
    assertThat(rm.nextPage()).isEqualTo(1);
  }
}
