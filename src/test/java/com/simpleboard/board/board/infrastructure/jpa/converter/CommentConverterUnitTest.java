package com.simpleboard.board.board.infrastructure.jpa.converter;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.simpleboard.board.board.domain.comment.dto.CommentCreateParams;
import com.simpleboard.board.board.domain.comment.entity.Comment;
import com.simpleboard.board.board.domain.comment.entity.GuestComment;
import com.simpleboard.board.board.domain.comment.entity.MemberComment;
import com.simpleboard.board.board.domain.comment.vo.CommentState;
import com.simpleboard.board.board.domain.comment.vo.CommentType;
import com.simpleboard.board.board.infrastructure.jpa.entity.CommentEntity;
import com.simpleboard.board.board.infrastructure.jpa.entity.GuestCommentEntity;
import com.simpleboard.board.board.infrastructure.jpa.entity.MemberCommentEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommentConverterUnitTest {

  private final CommentConverter converter = new CommentConverter();

  /* ====================================================================== */
  /* ============================ Domain -> JPA ============================ */
  /* ====================================================================== */

  @Test
  @DisplayName("toJpaEntity(): GuestComment → GuestCommentEntity 매핑")
  void toJpaEntity_guest_mapping() {
    // given
    CommentCreateParams params =
        CommentCreateParams.builder()
            .postId(11L)
            .parentCommentId(22L)
            .content("guest content")
            .commentType(CommentType.GUEST)
            .nickname("guestNick")
            .password("pw")
            .build();
    Comment guest = Comment.write(params);
    LocalDateTime created = LocalDateTime.now().minusHours(1);
    LocalDateTime updated = LocalDateTime.now();
    setField(guest, "id", 101L);
    setField(guest, "createdAt", created);
    setField(guest, "updatedAt", updated);

    // when
    CommentEntity entity = converter.toJpaEntity(guest);

    // then
    assertThat(entity).isInstanceOf(GuestCommentEntity.class);
    GuestCommentEntity ge = (GuestCommentEntity) entity;
    assertThat(ge.getId()).isEqualTo(101L);
    assertThat(ge.getParentId()).isEqualTo(22L);
    assertThat(ge.getPostId()).isEqualTo(11L);
    assertThat(ge.getContent()).isEqualTo("guest content");
    assertThat(ge.getCommentState()).isEqualTo(CommentState.ACTIVATE);
    assertThat(ge.getNickname()).isEqualTo("guestNick");
    assertThat(ge.getPassword()).isEqualTo("pw");
    assertThat(ge.getCreatedAt()).isEqualTo(created);
    assertThat(ge.getUpdatedAt()).isEqualTo(updated);
  }

  @Test
  @DisplayName("toJpaEntity(): MemberComment → MemberCommentEntity 매핑")
  void toJpaEntity_member_mapping() {
    // given
    CommentCreateParams params =
        CommentCreateParams.builder()
            .postId(33L)
            .parentCommentId(null)
            .content("member content")
            .commentType(CommentType.MEMBER)
            .writerId(1001L)
            .build();
    Comment member = Comment.write(params);
    LocalDateTime created = LocalDateTime.now().minusDays(1);
    LocalDateTime updated = LocalDateTime.now().minusHours(2);
    setField(member, "id", 202L);
    setField(member, "createdAt", created);
    setField(member, "updatedAt", updated);

    // when
    CommentEntity entity = converter.toJpaEntity(member);

    // then
    assertThat(entity).isInstanceOf(MemberCommentEntity.class);
    MemberCommentEntity me = (MemberCommentEntity) entity;
    assertThat(me.getId()).isEqualTo(202L);
    assertThat(me.getParentId()).isEqualTo(0);
    assertThat(me.getPostId()).isEqualTo(33L);
    assertThat(me.getContent()).isEqualTo("member content");
    assertThat(me.getCommentState()).isEqualTo(CommentState.ACTIVATE);
    assertThat(me.getWriterId()).isEqualTo(1001L);
    assertThat(me.getCreatedAt()).isEqualTo(created);
    assertThat(me.getUpdatedAt()).isEqualTo(updated);
  }

  @Test
  @DisplayName("toJpaEntity(): 지원되지 않는 도메인 타입 → IllegalArgumentException")
  void toJpaEntity_unknown_throws() {
    // given: 테스트용 미지원 타입
    class UnknownComment extends Comment {
      public UnknownComment(CommentCreateParams p) {
        super(p);
      }

      @Override
      protected void checkPermission(
          com.simpleboard.board.board.domain.common.vo.Visitor v,
          com.simpleboard.board.board.domain.comment.dto.CommentDeleteParams p) {}
    }
    CommentCreateParams params =
        CommentCreateParams.builder()
            .postId(1L)
            .content("x")
            .commentType(CommentType.GUEST)
            .nickname("n")
            .password("p")
            .build();
    Comment unknown = new UnknownComment(params);

    // when & then
    assertThatThrownBy(() -> converter.toJpaEntity(unknown))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /* ====================================================================== */
  /* ============================ JPA -> Domain ============================ */
  /* ====================================================================== */

  @Test
  @DisplayName("toDomainEntity(): GuestCommentEntity → GuestComment 매핑")
  void toDomainEntity_guest_mapping() {
    // given
    LocalDateTime created = LocalDateTime.now().minusHours(3);
    LocalDateTime updated = LocalDateTime.now();
    GuestCommentEntity entity =
        GuestCommentEntity.builder()
            .id(777L)
            .parentId(55L)
            .postId(44L)
            .content("guest entity content")
            .commentState(CommentState.ACTIVATE)
            .createdAt(created)
            .updatedAt(updated)
            .nickname("guestNick")
            .password("pw")
            .build();

    // when
    Comment domain = converter.toDomainEntity(entity);

    // then
    assertThat(domain).isInstanceOf(GuestComment.class);
    GuestComment gc = (GuestComment) domain;
    assertThat(gc.getId()).isEqualTo(777L);
    assertThat(gc.getParentId()).isEqualTo(55L);
    assertThat(gc.getPostId()).isEqualTo(44L);
    assertThat(gc.getContent()).isEqualTo("guest entity content");
    assertThat(gc.getCommentState()).isEqualTo(CommentState.ACTIVATE);
    assertThat(gc.getNickname()).isEqualTo("guestNick");
    assertThat(gc.getPassword()).isEqualTo("pw");
    assertThat(gc.getCreatedAt()).isEqualTo(created);
    assertThat(gc.getUpdatedAt()).isEqualTo(updated);
  }

  @Test
  @DisplayName("toDomainEntity(): MemberCommentEntity → MemberComment 매핑")
  void toDomainEntity_member_mapping() {
    // given
    LocalDateTime created = LocalDateTime.now().minusDays(2);
    LocalDateTime updated = LocalDateTime.now().minusDays(1);
    MemberCommentEntity entity =
        MemberCommentEntity.builder()
            .id(888L)
            .parentId(null)
            .postId(99L)
            .content("member entity content")
            .commentState(CommentState.DELETED) // 상태도 그대로 전달되는지 확인
            .createdAt(created)
            .updatedAt(updated)
            .writerId(1001L)
            .build();

    // when
    Comment domain = converter.toDomainEntity(entity);

    // then
    assertThat(domain).isInstanceOf(MemberComment.class);
    MemberComment mc = (MemberComment) domain;
    assertThat(mc.getId()).isEqualTo(888L);
    assertThat(mc.getParentId()).isNull();
    assertThat(mc.getPostId()).isEqualTo(99L);
    assertThat(mc.getContent()).isEqualTo("member entity content");
    assertThat(mc.getCommentState()).isEqualTo(CommentState.DELETED);
    assertThat(mc.getWriterId()).isEqualTo(1001L);
    assertThat(mc.getCreatedAt()).isEqualTo(created);
    assertThat(mc.getUpdatedAt()).isEqualTo(updated);
  }

  @Test
  @DisplayName("toDomainEntity(): 지원되지 않는 JPA 타입 → IllegalArgumentException")
  void toDomainEntity_unknown_throws() {
    // given: 테스트용 미지원 JPA 엔티티 (필드 값은 리플렉션으로 세팅)
    class UnknownCommentEntity extends CommentEntity {}
    UnknownCommentEntity unknown = new UnknownCommentEntity();
    setField(unknown, "id", 1L);
    setField(unknown, "parentId", 2L);
    setField(unknown, "postId", 3L);
    setField(unknown, "content", "x");
    setField(unknown, "commentState", CommentState.ACTIVATE);

    // when & then
    assertThatThrownBy(() -> converter.toDomainEntity(unknown))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /* ====================================================================== */
  /* ============================== Round Trip ============================ */
  /* ====================================================================== */

  @Test
  @DisplayName("Round-trip: GuestComment → JPA → Domain 필드 동일성")
  void roundTrip_guest() {
    // given
    CommentCreateParams params =
        CommentCreateParams.builder()
            .postId(10L)
            .parentCommentId(9L)
            .content("rr guest")
            .commentType(CommentType.GUEST)
            .nickname("gn")
            .password("pp")
            .build();
    Comment guest = Comment.write(params);
    setField(guest, "id", 1000L);
    setField(guest, "commentState", CommentState.ACTIVATE);

    // when
    CommentEntity jpa = converter.toJpaEntity(guest);
    Comment back = converter.toDomainEntity(jpa);

    // then
    assertThat(back).isInstanceOf(GuestComment.class);
    assertThat(back.getId()).isEqualTo(guest.getId());
    assertThat(back.getParentId()).isEqualTo(guest.getParentId());
    assertThat(back.getPostId()).isEqualTo(guest.getPostId());
    assertThat(back.getContent()).isEqualTo(guest.getContent());
    assertThat(back.getCommentState()).isEqualTo(guest.getCommentState());
    assertThat(((GuestComment) back).getNickname()).isEqualTo(((GuestComment) guest).getNickname());
    assertThat(((GuestComment) back).getPassword()).isEqualTo(((GuestComment) guest).getPassword());
  }
}
