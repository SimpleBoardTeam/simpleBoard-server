package com.simpleboard.board.board.domain.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.simpleboard.board.board.domain.comment.dto.CommentCreateParams;
import com.simpleboard.board.board.domain.comment.dto.CommentDeleteParams;
import com.simpleboard.board.board.domain.comment.entity.Comment;
import com.simpleboard.board.board.domain.comment.entity.GuestComment;
import com.simpleboard.board.board.domain.comment.entity.MemberComment;
import com.simpleboard.board.board.domain.comment.vo.CommentState;
import com.simpleboard.board.board.domain.comment.vo.CommentType;
import com.simpleboard.board.board.domain.common.vo.Visitor;
import com.simpleboard.board.board.domain.common.vo.VisitorType;
import com.simpleboard.board.board.infrastructure.jpa.converter.CommentConverter;
import com.simpleboard.board.board.infrastructure.jpa.entity.CommentEntity;
import com.simpleboard.board.board.infrastructure.jpa.repository.CommentCommandRepositoryImpl;
import com.simpleboard.board.board.infrastructure.jpa.repository.CommentEntityRepository;
import com.simpleboard.board.testconfig.JpaAuditTestConfig;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * <b>Repository 테스트</b>
 *
 * <p>H2 DB를 활용한 JPA 레포지토리 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({CommentCommandRepositoryImpl.class, CommentConverter.class, JpaAuditTestConfig.class})
class CommentCommandRepositoryTest {

  private static final Logger log = LoggerFactory.getLogger(CommentCommandRepositoryTest.class);
  @Autowired CommentCommandRepository repository;
  @Autowired CommentEntityRepository entityRepository;

  @Nested
  @DisplayName("save")
  class SaveTests {

    @Test
    @DisplayName("GuestComment 를 저장하면 ID가 채워지고 필드가 보존된다")
    void save_guest() {
      // given
      Comment comment = Comment.write(guestParams());

      // when
      Comment saved = repository.save(comment);

      // then
      assertThat(saved).isInstanceOf(GuestComment.class);
      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getPostId()).isEqualTo(100L);
      assertThat(saved.getParentId()).isNull();
      assertThat(saved.getContent()).isEqualTo("hello-guest");
      assertThat(saved.getCommentState()).isEqualTo(CommentState.ACTIVATE);

      GuestComment gc = (GuestComment) saved;
      assertThat(gc.getNickname()).isEqualTo("게스트닉");
      assertThat(gc.getPassword()).isEqualTo("pass-1234");
    }

    @Test
    @DisplayName("MemberComment 를 저장하면 ID가 채워지고 필드가 보존된다")
    void save_member() {
      // given
      Comment comment = Comment.write(memberParams());

      // when
      Comment saved = repository.save(comment);

      // then
      assertThat(saved).isInstanceOf(MemberComment.class);
      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getPostId()).isEqualTo(200L);
      assertThat(saved.getParentId()).isEqualTo(10L);
      assertThat(saved.getContent()).isEqualTo("hello-member");
      assertThat(saved.getCommentState()).isEqualTo(CommentState.ACTIVATE);

      MemberComment mc = (MemberComment) saved;
      assertThat(mc.getWriterId()).isEqualTo(42L);
    }
  }

  @Nested
  @DisplayName("findById")
  class FindTests {

    @Test
    @DisplayName("저장 후 ID로 조회하면 동일 타입/값으로 변환되어 돌아온다 (Guest)")
    void find_guest_roundtrip() {
      // given
      Comment saved = repository.save(Comment.write(guestParams()));

      // when
      Optional<Comment> foundOpt = repository.findById(saved.getId());

      // then
      assertThat(foundOpt).isPresent();
      Comment found = foundOpt.get();
      assertThat(found).isInstanceOf(GuestComment.class);
      assertThat(found.getId()).isEqualTo(saved.getId());
      assertThat(found.getContent()).isEqualTo("hello-guest");
      assertThat(((GuestComment) found).getNickname()).isEqualTo("게스트닉");
    }

    @Test
    @DisplayName("저장 후 삭제 API 시나리오를 흉내내면 상태가 DELETED로 바뀐다 (Member)")
    void delete_member_flow_like_service() {
      // given: 저장
      MemberComment saved = (MemberComment) repository.save(Comment.write(memberParams()));

      // when: 도메인 삭제 로직 실행 (권한자)
      Visitor writer =
          Visitor.builder().type(VisitorType.MEMBER).memberId(42L).vId("vid-xxx").build();
      saved.deleteComment(writer, CommentDeleteParams.builder().password(null).build());
      Comment after = repository.save(saved);

      // then
      assertThat(after.getCommentState()).isEqualTo(CommentState.DELETED);
      // 조회해서도 삭제 상태 유지 확인
      Comment found = repository.findById(after.getId()).orElseThrow();
      assertThat(found.getCommentState()).isEqualTo(CommentState.DELETED);
    }

    @Test
    @DisplayName("존재하지 않는 ID 조회시 Optional.empty")
    void not_found() {
      assertThat(repository.findById(-999L)).isEmpty();
    }
  }

  @Nested
  @DisplayName("Insert Concurrency")
  class ConcurrencyTest {

    private final int THREADS = 10;

    @Test
    @DisplayName("sibling_seq 동시성 테스트 - Root Comment")
    void insert_parent_comment_at_once() throws InterruptedException {

      ExecutorService pool = Executors.newFixedThreadPool(THREADS);
      CyclicBarrier startLine = new CyclicBarrier(THREADS);
      CountDownLatch done = new CountDownLatch(THREADS);

      List<Future<Long>> futures = new ArrayList<>(THREADS);

      for (int i = 0; i < THREADS; i++) {
        futures.add(
            pool.submit(
                () -> {
                  try {
                    startLine.await(100, TimeUnit.MILLISECONDS);
                  } catch (Exception ignored) {
                    log.debug("setting error");
                  }
                  try {
                    Comment comment = Comment.write(guestParams());
                    return repository.save(comment).getId();
                  } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                  } finally {
                    done.countDown();
                  }
                }));
      }

      done.await(1, TimeUnit.SECONDS);
      pool.shutdown();

      assertThat(futures.stream().filter(f -> !f.isDone()).count()).isEqualTo(0);

      Set<Integer> set = new HashSet<>();
      entityRepository.findAll().stream()
          .forEach(
              c -> {
                assertThat(set.contains(c.getSiblingSeq())).isFalse();
                set.add(c.getSiblingSeq());
              });
      assertThat(set.size()).isEqualTo(THREADS);
    }

    @Test
    @DisplayName("sibling_seq 동시성 테스트 - Child Comment")
    void insert_child_comment_at_once() throws InterruptedException {

      ExecutorService pool = Executors.newFixedThreadPool(THREADS);
      CyclicBarrier startLine = new CyclicBarrier(THREADS);
      CountDownLatch done = new CountDownLatch(THREADS);

      List<Future<Long>> futures = new ArrayList<>(THREADS);

      Comment parent = Comment.write(guestParams());
      Comment saved = repository.save(parent);
      Long parentId = saved.getId();

      for (int i = 0; i < THREADS; i++) {
        futures.add(
            pool.submit(
                () -> {
                  try {
                    startLine.await(100, TimeUnit.MILLISECONDS);
                  } catch (Exception ignored) {
                    log.debug("setting error");
                  }
                  try {
                    Comment comment = Comment.write(guestParams(parentId));
                    return repository.save(comment).getId();
                  } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                  } finally {
                    done.countDown();
                  }
                }));
      }

      done.await(1, TimeUnit.SECONDS);
      pool.shutdown();

      Set<Integer> set = new HashSet<>();
      List<CommentEntity> all = entityRepository.findAll();

      assertThat(futures.stream().filter(f -> !f.isDone()).count()).isEqualTo(0);

      all.stream()
          .filter(c -> c.getParentId().equals(parentId))
          .forEach(
              c -> {
                assertThat(set.contains(c.getSiblingSeq())).isFalse();
                set.add(c.getSiblingSeq());
              });

      assertThat(set.size()).isEqualTo(THREADS);
    }
  }

  private CommentCreateParams guestParams() {
    return CommentCreateParams.builder()
        .postId(100L)
        .parentCommentId(null)
        .content("hello-guest")
        .commentType(CommentType.GUEST)
        .nickname("게스트닉")
        .password("pass-1234")
        .build();
  }

  private CommentCreateParams guestParams(Long parentId) {
    return CommentCreateParams.builder()
        .postId(100L)
        .parentCommentId(parentId)
        .content("hello-guest-child")
        .commentType(CommentType.GUEST)
        .nickname("child")
        .password("pass-1234")
        .build();
  }

  private CommentCreateParams memberParams() {
    return CommentCreateParams.builder()
        .postId(200L)
        .parentCommentId(10L)
        .content("hello-member")
        .commentType(CommentType.MEMBER)
        .writerId(42L)
        .build();
  }
}
