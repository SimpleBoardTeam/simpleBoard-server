package com.simpleboard.board.board.infrastructure.jpa.repository;

import com.simpleboard.board.board.domain.comment.entity.Comment;
import com.simpleboard.board.board.domain.comment.repository.CommentCommandRepository;
import com.simpleboard.board.board.infrastructure.jpa.converter.CommentConverter;
import com.simpleboard.board.board.infrastructure.jpa.entity.CommentEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * <b>Board B.C Comment Command Repository 구현체</b>
 *
 * <p>Domain entity <-> JPA entity 간의 변환을 캡슐화하여 내부적으로 처리
 *
 * <p>Domain entity의 저장, 수정, 조회 수행
 */
@Component
@RequiredArgsConstructor
public class CommentCommandRepositoryImpl implements CommentCommandRepository {

  private static final Logger log = LoggerFactory.getLogger(CommentCommandRepositoryImpl.class);
  private final CommentEntityRepository commentEntityRepository;
  private final CommentConverter converter;

  private final Integer RETRY_LIMIT = 10;

  @Override
  public Comment save(Comment comment) {
    if (comment.getId() != null) {
      CommentEntity saved = commentEntityRepository.save(converter.toJpaEntity(comment));
      return converter.toDomainEntity(saved);
    }
    CommentEntity entity = converter.toJpaEntity(comment);

    int attempt = 0;
    while (true) {
      try {
        Integer lastSeq = commentEntityRepository.maxSiblingSeqByParentId(entity.getParentId());
        lastSeq = lastSeq != null ? lastSeq : 0;

        entity.setSiblingSeq(lastSeq + 1);

        return converter.toDomainEntity(commentEntityRepository.save(entity));
      } catch (DataIntegrityViolationException e) {
        attempt++;
        if (attempt >= RETRY_LIMIT) throw e;
        log.info("Parent ID: {}  Attempt {}/{} failed", comment.getParentId(), attempt, RETRY_LIMIT);
      }
    }
  }

  @Override
  public Optional<Comment> findById(Long id) {
    CommentEntity comment = commentEntityRepository.findById(id).orElse(null);
    if (comment == null) return Optional.empty();
    return Optional.of(converter.toDomainEntity(comment));
  }
}
