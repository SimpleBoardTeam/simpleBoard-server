package com.simpleboard.board.board.infrastructure.jpa.repository;

import com.simpleboard.board.board.infrastructure.jpa.entity.CommentEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * <b>Comment aggregate의 JPA 레포지토리</b>
 *
 * <p>Comment JPA 엔티티의 저장 및 조회 담당
 */
public interface CommentEntityRepository extends JpaRepository<CommentEntity, Long> {
  @Override
  Optional<CommentEntity> findById(Long aLong);

  CommentEntity save(CommentEntity commentEntity);

  @Query(
      "select c.siblingSeq from CommentEntity c where c.parentId = :parentId"
          + " order by c.siblingSeq desc limit 1")
  Integer maxSiblingSeqByParentId(Long parentId);
}
