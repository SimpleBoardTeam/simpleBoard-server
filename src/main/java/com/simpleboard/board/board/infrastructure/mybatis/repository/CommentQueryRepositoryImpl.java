package com.simpleboard.board.board.infrastructure.mybatis.repository;

import com.simpleboard.board.board.application.query.CommentListCriteria;
import com.simpleboard.board.board.application.query.CommentListReadModel;
import com.simpleboard.board.board.application.query.CommentQueryRepository;
import com.simpleboard.board.board.infrastructure.mybatis.converter.CommentQueryConverter;
import com.simpleboard.board.board.infrastructure.mybatis.mapper.CommentListCondition;
import com.simpleboard.board.board.infrastructure.mybatis.mapper.CommentQueryMapper;
import com.simpleboard.board.board.infrastructure.mybatis.mapper.CommentSummaryData;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentQueryRepositoryImpl implements CommentQueryRepository {
  private final CommentQueryMapper commentQueryMapper;
  private final CommentQueryConverter converter;

  @Override
  public CommentListReadModel getCommentList(CommentListCriteria criteria) {

    CommentListCondition condition = converter.toCondition(criteria);
    List<CommentSummaryData> commentList = commentQueryMapper.getCommentList(condition);
    Long activeComments = commentQueryMapper.countActiveComments(criteria.postId());
    Long rootComments = commentQueryMapper.countRootComments(criteria.postId());
    return converter.toReadModel(commentList, activeComments, rootComments, condition);
  }
}
